package com.ucms_backend.service;

import com.ucms_backend.dto.CreateResponseRequest;
import com.ucms_backend.dto.TicketResponseDto;
import com.ucms_backend.exception.AppException;
import com.ucms_backend.model.entity.Ticket;
import com.ucms_backend.model.entity.TicketResponse;
import com.ucms_backend.repository.TicketRepository;
import com.ucms_backend.repository.TicketResponseRepository;
import com.ucms_backend.security.SecurityUtils;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TicketResponseService {

    private final TicketRepository ticketRepository;
    private final TicketResponseRepository ticketResponseRepository;
    private final NotificationService notificationService;

    public TicketResponseService(
            TicketRepository ticketRepository,
            TicketResponseRepository ticketResponseRepository,
            NotificationService notificationService
    ) {
        this.ticketRepository = ticketRepository;
        this.ticketResponseRepository = ticketResponseRepository;
        this.notificationService = notificationService;
    }

    public TicketResponseDto addResponse(Long ticketId, CreateResponseRequest request) {
        UUID adminId = SecurityUtils.getCurrentUserId();

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new AppException(404, "TICKET_NOT_FOUND", "Ticket not found"));

        TicketResponse response = TicketResponse.builder()
                .ticketId(ticket.getId())
                .adminId(adminId)
                .message(request.getMessage())
                .build();

        TicketResponse saved = ticketResponseRepository.save(response);

        notificationService.createNotification(
                ticket.getUserId(),
                ticket.getId(),
                "Admin posted a response to your ticket #" + ticket.getTicketNumber()
        );

        return TicketResponseDto.from(saved);
    }

    public List<TicketResponseDto> getResponses(Long ticketId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentRole();

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new AppException(404, "TICKET_NOT_FOUND", "Ticket not found"));

        if ("STUDENT".equals(role) && !ticket.getUserId().equals(userId)) {
            throw new AppException(403, "FORBIDDEN", "Access denied");
        }

        return ticketResponseRepository.findByTicketIdOrderByCreatedAtAscIdAsc(ticketId).stream()
                .map(TicketResponseDto::from)
                .toList();
    }
}
