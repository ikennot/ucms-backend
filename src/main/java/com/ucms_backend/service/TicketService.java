package com.ucms_backend.service;

import com.ucms_backend.dto.CreateTicketRequest;
import com.ucms_backend.dto.TicketResponse;
import com.ucms_backend.dto.UpdateStatusRequest;
import com.ucms_backend.exception.AppException;
import com.ucms_backend.model.entity.Profile;
import com.ucms_backend.model.entity.Ticket;
import com.ucms_backend.model.enums.TicketStatus;
import com.ucms_backend.repository.CategoryRepository;
import com.ucms_backend.repository.ProfileRepository;
import com.ucms_backend.repository.TicketRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class TicketService {

    private static final Map<TicketStatus, TicketStatus> VALID_TRANSITIONS = Map.of(
            TicketStatus.PENDING, TicketStatus.IN_PROGRESS,
            TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED,
            TicketStatus.RESOLVED, TicketStatus.CLOSED
    );

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final ProfileRepository profileRepository;
    private final TicketNumberGenerator ticketNumberGenerator;
    private final NotificationService notificationService;

    public TicketService(
            TicketRepository ticketRepository,
            CategoryRepository categoryRepository,
            ProfileRepository profileRepository,
            TicketNumberGenerator ticketNumberGenerator,
            NotificationService notificationService
    ) {
        this.ticketRepository = ticketRepository;
        this.categoryRepository = categoryRepository;
        this.profileRepository = profileRepository;
        this.ticketNumberGenerator = ticketNumberGenerator;
        this.notificationService = notificationService;
    }

    public TicketResponse createTicket(UUID userId, CreateTicketRequest request) {
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new AppException(404, "PROFILE_NOT_FOUND", "Profile not found"));

        if (!profile.isEmailVerified()) {
            throw new AppException(403, "ACCOUNT_LIMITED", "Verified email required to create tickets");
        }

        if (!categoryRepository.existsById(request.getCategoryId())) {
            throw new AppException(404, "CATEGORY_NOT_FOUND", "Category not found");
        }

        String ticketNumber = ticketNumberGenerator.generate();

        Ticket ticket = Ticket.builder()
                .userId(userId)
                .categoryId(request.getCategoryId())
                .ticketNumber(ticketNumber)
                .title(request.getTitle())
                .description(request.getDescription())
                .build();

        Ticket saved = ticketRepository.save(ticket);
        return TicketResponse.from(saved);
    }

    public List<TicketResponse> getTickets(UUID userId, String role, String status, Long categoryId) {
        if ("STUDENT".equals(role)) {
            return ticketRepository.findByUserId(userId).stream()
                    .map(TicketResponse::from)
                    .toList();
        }

        if ("ADMIN".equals(role)) {
            Specification<Ticket> spec = (root, query, cb) -> cb.conjunction();

            if (status != null) {
                TicketStatus ticketStatus = TicketStatus.valueOf(status);
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), ticketStatus));
            }

            if (categoryId != null) {
                spec = spec.and((root, query, cb) -> cb.equal(root.get("categoryId"), categoryId));
            }

            return ticketRepository.findAll(spec).stream()
                    .map(TicketResponse::from)
                    .toList();
        }

        return List.of();
    }

    public TicketResponse getTicketById(Long id, UUID userId, String role) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new AppException(404, "TICKET_NOT_FOUND", "Ticket not found"));

        if ("STUDENT".equals(role) && !ticket.getUserId().equals(userId)) {
            throw new AppException(403, "FORBIDDEN", "Access denied");
        }

        return TicketResponse.from(ticket);
    }

    public TicketResponse updateStatus(Long id, UpdateStatusRequest request) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new AppException(404, "TICKET_NOT_FOUND", "Ticket not found"));

        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new AppException(403, "TICKET_CLOSED", "Cannot modify a closed ticket");
        }

        TicketStatus next;
        try {
            next = TicketStatus.valueOf(request.getStatus());
        } catch (IllegalArgumentException ex) {
            throw new AppException(400, "INVALID_STATUS_TRANSITION", "Invalid status transition");
        }

        TicketStatus current = ticket.getStatus();
        if (!next.equals(VALID_TRANSITIONS.get(current))) {
            throw new AppException(400, "INVALID_STATUS_TRANSITION", "Invalid status transition");
        }

        ticket.setStatus(next);
        Ticket saved = ticketRepository.save(ticket);

        notificationService.createNotification(
                saved.getUserId(),
                saved.getId(),
                "Your ticket #" + saved.getTicketNumber() + " status has been updated to " + next.name()
        );

        return TicketResponse.from(saved);
    }
}
