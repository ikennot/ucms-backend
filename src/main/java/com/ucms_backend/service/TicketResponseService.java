package com.ucms_backend.service;

import com.ucms_backend.dto.CreateResponseRequest;
import com.ucms_backend.dto.RealtimeEventResponse;
import com.ucms_backend.dto.TicketResponseDto;
import com.ucms_backend.exception.AppException;
import com.ucms_backend.model.entity.Profile;
import com.ucms_backend.model.entity.Ticket;
import com.ucms_backend.model.entity.TicketResponse;
import com.ucms_backend.model.enums.TicketStatus;
import com.ucms_backend.repository.ProfileRepository;
import com.ucms_backend.repository.TicketRepository;
import com.ucms_backend.repository.TicketResponseRepository;
import com.ucms_backend.security.SecurityUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;

@Service
public class TicketResponseService {

    private final TicketRepository ticketRepository;
    private final TicketResponseRepository ticketResponseRepository;
    private final ProfileRepository profileRepository;
    private final NotificationService notificationService;
    private final RealtimeSseService realtimeSseService;

    public TicketResponseService(
            TicketRepository ticketRepository,
            TicketResponseRepository ticketResponseRepository,
            ProfileRepository profileRepository,
            NotificationService notificationService,
            RealtimeSseService realtimeSseService
    ) {
        this.ticketRepository = ticketRepository;
        this.ticketResponseRepository = ticketResponseRepository;
        this.profileRepository = profileRepository;
        this.notificationService = notificationService;
        this.realtimeSseService = realtimeSseService;
    }

    public TicketResponseDto addResponse(Long ticketId, CreateResponseRequest request) {
        UUID callerId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentRole();

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new AppException(404, "TICKET_NOT_FOUND", "Ticket not found"));

        TicketResponse.TicketResponseBuilder builder = TicketResponse.builder()
                .ticketId(ticket.getId())
                .message(request.getMessage())
                .ticketStatus(ticket.getStatus().name())
                .responderRole(role);

        if ("STUDENT".equals(role)) {
            if (!ticket.getUserId().equals(callerId)) {
                throw new AppException(403, "FORBIDDEN", "Access denied");
            }
            if (ticket.getStatus() != TicketStatus.IN_PROGRESS) {
                throw new AppException(400, "INVALID_STATUS",
                        "You can only add comments while your ticket is in progress");
            }
            builder.studentId(callerId);
        } else {
            builder.adminId(callerId);
        }

        TicketResponse saved = ticketResponseRepository.save(builder.build());

        Profile senderProfile = profileRepository.findById(callerId).orElse(null);
        String senderName = senderProfile != null ? senderProfile.getName() : null;

        if ("STUDENT".equals(role)) {
            // Notify assigned admin (or all admins) that the student replied
            if (ticket.getAssignedAdminId() != null) {
                notificationService.createNotification(
                        ticket.getAssignedAdminId(),
                        ticket.getId(),
                        "Student replied on ticket #" + ticket.getTicketNumber()
                );
            }
        } else {
            notificationService.createNotification(
                    ticket.getUserId(),
                    ticket.getId(),
                    "Admin posted a response to your ticket #" + ticket.getTicketNumber()
            );
        }

        RealtimeEventResponse event = RealtimeEventResponse.builder()
                .domain("tickets")
                .eventType("TICKET_RESPONSE_CREATED")
                .entityId(String.valueOf(ticket.getId()))
                .updatedAt(LocalDateTime.now(ZoneOffset.UTC))
                .actorRole(role)
                .build();
        realtimeSseService.publishToUser(ticket.getUserId(), event);
        realtimeSseService.publishToAdmins(event);

        return TicketResponseDto.from(saved, senderName);
    }

    public List<TicketResponseDto> getResponses(Long ticketId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentRole();

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new AppException(404, "TICKET_NOT_FOUND", "Ticket not found"));

        if ("STUDENT".equals(role) && !ticket.getUserId().equals(userId)) {
            throw new AppException(403, "FORBIDDEN", "Access denied");
        }

        List<TicketResponse> responses =
                ticketResponseRepository.findByTicketIdOrderByCreatedAtAscIdAsc(ticketId);

        // Collect all unique poster IDs to bulk-fetch profiles
        Map<UUID, String> profileNames = new HashMap<>();
        List<UUID> profileIds = new ArrayList<>();
        for (TicketResponse r : responses) {
            if (r.getAdminId() != null) profileIds.add(r.getAdminId());
            if (r.getStudentId() != null) profileIds.add(r.getStudentId());
        }
        if (!profileIds.isEmpty()) {
            profileRepository.findAllById(profileIds)
                    .forEach(p -> profileNames.put(p.getAuthUserId(), p.getName()));
        }

        return responses.stream()
                .map(r -> {
                    UUID posterId = "STUDENT".equals(r.getResponderRole())
                            ? r.getStudentId() : r.getAdminId();
                    String name = posterId != null ? profileNames.get(posterId) : null;
                    return TicketResponseDto.from(r, name);
                })
                .toList();
    }
}
