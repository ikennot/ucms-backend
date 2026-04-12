package com.ucms_backend.service;

import com.ucms_backend.dto.AssignTicketRequest;
import com.ucms_backend.dto.CreateTicketRequest;
import com.ucms_backend.dto.RealtimeEventResponse;
import com.ucms_backend.dto.TicketResponse;
import com.ucms_backend.dto.UrgencyOverrideRequest;
import com.ucms_backend.dto.UpdateStatusRequest;
import com.ucms_backend.exception.AppException;
import com.ucms_backend.model.entity.Category;
import com.ucms_backend.model.entity.Profile;
import com.ucms_backend.model.entity.Ticket;
import com.ucms_backend.model.enums.TicketStatus;
import com.ucms_backend.repository.CategoryRepository;
import com.ucms_backend.repository.ProfileRepository;
import com.ucms_backend.repository.TicketRepository;
import com.ucms_backend.repository.TicketResponseRepository;
import com.ucms_backend.security.SecurityUtils;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
    private final TicketResponseRepository ticketResponseRepository;
    private final TicketNumberGenerator ticketNumberGenerator;
    private final NotificationService notificationService;
    private final TicketUrgencyScoringService ticketUrgencyScoringService;
    private final RealtimeSseService realtimeSseService;

    public TicketService(
            TicketRepository ticketRepository,
            CategoryRepository categoryRepository,
            ProfileRepository profileRepository,
            TicketResponseRepository ticketResponseRepository,
            TicketNumberGenerator ticketNumberGenerator,
            NotificationService notificationService,
            TicketUrgencyScoringService ticketUrgencyScoringService,
            RealtimeSseService realtimeSseService
    ) {
        this.ticketRepository = ticketRepository;
        this.categoryRepository = categoryRepository;
        this.profileRepository = profileRepository;
        this.ticketResponseRepository = ticketResponseRepository;
        this.ticketNumberGenerator = ticketNumberGenerator;
        this.notificationService = notificationService;
        this.ticketUrgencyScoringService = ticketUrgencyScoringService;
        this.realtimeSseService = realtimeSseService;
    }

    private String resolveCategoryName(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .map(Category::getName)
                .orElse(null);
    }

    private String resolveAdminName(UUID adminId) {
        if (adminId == null) return null;
        return profileRepository.findById(adminId)
                .map(Profile::getName)
                .orElse(null);
    }

    private TicketResponse toResponse(Ticket ticket) {
        boolean hasAdminResponse = ticketResponseRepository.existsByTicketId(ticket.getId());
        return TicketResponse.from(ticket, resolveCategoryName(ticket.getCategoryId()), null, hasAdminResponse,
                resolveAdminName(ticket.getAssignedAdminId()));
    }

    private TicketResponse toResponse(Ticket ticket, Set<Long> ticketIdsWithResponses) {
        boolean hasAdminResponse = ticketIdsWithResponses.contains(ticket.getId());
        return TicketResponse.from(ticket, resolveCategoryName(ticket.getCategoryId()), null, hasAdminResponse,
                resolveAdminName(ticket.getAssignedAdminId()));
    }

    private TicketResponse toDetailedResponse(Ticket ticket) {
        Profile studentProfile = profileRepository.findById(ticket.getUserId()).orElse(null);
        boolean hasAdminResponse = ticketResponseRepository.existsByTicketId(ticket.getId());
        return TicketResponse.from(ticket, resolveCategoryName(ticket.getCategoryId()), studentProfile, hasAdminResponse,
                resolveAdminName(ticket.getAssignedAdminId()));
    }

    private void applyUrgency(Ticket ticket) {
        if (ticket.isUrgencyOverridden()) {
            return;
        }
        TicketUrgencyEvaluation evaluation = ticketUrgencyScoringService.evaluate(ticket, resolveCategoryName(ticket.getCategoryId()));
        ticket.setUrgencyScore(evaluation.score());
        ticket.setUrgencyLabel(evaluation.priorityLevel());
        ticket.setUrgencyReason(evaluation.reason());
        ticket.setUrgencySignals(evaluation.signals());
        ticket.setUrgencyConfidence(evaluation.confidence());
        ticket.setUrgencyUpdatedAt(evaluation.updatedAt() != null ? evaluation.updatedAt() : LocalDateTime.now(ZoneOffset.UTC));
    }

    public TicketResponse overrideUrgency(Long ticketId, UrgencyOverrideRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new AppException(404, "TICKET_NOT_FOUND", "Ticket not found"));

        String normalizedPriority = normalizeOverridePriority(request.getPriorityLevel());
        int score = scoreForPriority(normalizedPriority);
        String reason = request.getReason() != null ? request.getReason().trim() : "";
        if (reason.length() > 500) {
            reason = reason.substring(0, 500);
        }
        if (reason.isEmpty()) {
            reason = "Urgency manually overridden by admin";
        }

        ticket.setUrgencyOverridden(true);
        ticket.setUrgencyOverrideReason(reason);
        ticket.setUrgencyLabel(normalizedPriority);
        ticket.setUrgencyScore(score);
        ticket.setUrgencyReason("Manual admin override applied");
        ticket.setUrgencySignals("Manual override");
        ticket.setUrgencyConfidence(1.0);
        ticket.setUrgencyUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));

        Ticket saved = ticketRepository.save(ticket);
        return toDetailedResponse(saved);
    }

    private String normalizeOverridePriority(String value) {
        if (value == null) {
            throw new AppException(400, "INVALID_PRIORITY_LEVEL", "priorityLevel is required");
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (Set.of("CRITICAL", "HIGH", "LOW", "MUTED").contains(normalized)) {
            return normalized;
        }
        throw new AppException(400, "INVALID_PRIORITY_LEVEL", "Allowed values: MUTED, LOW, HIGH, CRITICAL");
    }

    private int scoreForPriority(String priorityLevel) {
        return switch (priorityLevel) {
            case "CRITICAL" -> 90;
            case "HIGH" -> 70;
            case "LOW" -> 40;
            default -> 10;
        };
    }

    private Set<Long> findTicketIdsWithResponses(List<Ticket> tickets) {
        List<Long> ticketIds = tickets.stream()
                .map(Ticket::getId)
                .toList();

        if (ticketIds.isEmpty()) {
            return Set.of();
        }

        List<Long> foundIds = ticketResponseRepository.findDistinctTicketIdsByTicketIdIn(ticketIds);
        if (foundIds == null || foundIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(foundIds);
    }

    private void notifyAdmins(Ticket ticket, String message) {
        List<Profile> admins = profileRepository.findByRole("ADMIN");
        for (Profile admin : admins) {
            if (admin != null && admin.getAuthUserId() != null) {
                notificationService.createNotification(admin.getAuthUserId(), ticket.getId(), message);
            }
        }
    }

    public TicketResponse createTicket(CreateTicketRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
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

        applyUrgency(ticket);
        Ticket saved = ticketRepository.save(ticket);
        publishTicketEvent(saved, "TICKET_CREATED", "STUDENT");
        notifyAdmins(saved, "New ticket submitted: #" + saved.getTicketNumber());
        return toResponse(saved);
    }

    public List<TicketResponse> getTickets(String status, Long categoryId, boolean includeArchived) {
        UUID userId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentRole();

        if ("STUDENT".equals(role)) {
            List<Ticket> tickets = includeArchived
                    ? ticketRepository.findByUserId(userId)
                    : ticketRepository.findByUserIdAndArchivedFalse(userId);
            Set<Long> ticketIdsWithResponses = findTicketIdsWithResponses(tickets);
            return tickets.stream()
                    .map(ticket -> toResponse(ticket, ticketIdsWithResponses))
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

            if (!includeArchived) {
                spec = spec.and((root, query, cb) -> cb.isFalse(root.get("archived")));
            }

            List<Ticket> tickets = ticketRepository.findAll(spec);
            Set<Long> ticketIdsWithResponses = findTicketIdsWithResponses(tickets);
            return tickets.stream()
                    .map(ticket -> toResponse(ticket, ticketIdsWithResponses))
                    .toList();
        }

        return List.of();
    }

    public List<TicketResponse> getTicketsSince(Instant since, boolean includeArchived) {
        UUID userId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentRole();
        LocalDateTime sinceUtc = since != null ? LocalDateTime.ofInstant(since, ZoneOffset.UTC) : null;

        if ("STUDENT".equals(role)) {
            List<Ticket> tickets = includeArchived
                    ? ticketRepository.findByUserId(userId)
                    : ticketRepository.findByUserIdAndArchivedFalse(userId);

            if (sinceUtc != null) {
                tickets = tickets.stream()
                        .filter(ticket -> ticket.getUpdatedAt() != null && !ticket.getUpdatedAt().isBefore(sinceUtc))
                        .toList();
            }

            Set<Long> ticketIdsWithResponses = findTicketIdsWithResponses(tickets);
            return tickets.stream()
                    .map(ticket -> toResponse(ticket, ticketIdsWithResponses))
                    .toList();
        }

        if ("ADMIN".equals(role)) {
            Specification<Ticket> spec = (root, query, cb) -> cb.conjunction();

            if (!includeArchived) {
                spec = spec.and((root, query, cb) -> cb.isFalse(root.get("archived")));
            }

            if (sinceUtc != null) {
                spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("updatedAt"), sinceUtc));
            }

            List<Ticket> tickets = ticketRepository.findAll(spec);
            Set<Long> ticketIdsWithResponses = findTicketIdsWithResponses(tickets);
            return tickets.stream()
                    .map(ticket -> toResponse(ticket, ticketIdsWithResponses))
                    .toList();
        }

        return List.of();
    }

    public TicketResponse getTicketById(Long id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentRole();

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new AppException(404, "TICKET_NOT_FOUND", "Ticket not found"));

        if ("STUDENT".equals(role) && !ticket.getUserId().equals(userId)) {
            throw new AppException(403, "FORBIDDEN", "Access denied");
        }

        return toDetailedResponse(ticket);
    }

    public TicketResponse confirmResolved(Long ticketId) {
        UUID userId = SecurityUtils.getCurrentUserId();

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new AppException(404, "TICKET_NOT_FOUND", "Ticket not found"));

        if (!ticket.getUserId().equals(userId)) {
            throw new AppException(403, "FORBIDDEN", "Access denied");
        }

        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new AppException(409, "INVALID_STATUS_TRANSITION",
                    "Ticket must be in RESOLVED status to confirm resolution");
        }

        if (ticket.isConfirmedResolved()) {
            throw new AppException(409, "ALREADY_CONFIRMED", "Ticket resolution already confirmed");
        }

        ticket.setConfirmedResolved(true);
        ticket.setStatus(TicketStatus.CLOSED);
        ticket.setArchived(true);
        applyUrgency(ticket);
        Ticket saved = ticketRepository.save(ticket);
        publishTicketEvent(saved, "TICKET_CONFIRMED_RESOLVED", "STUDENT");

        notificationService.createNotification(
                saved.getUserId(),
                saved.getId(),
                "You have confirmed your ticket as resolved."
        );

        notifyAdmins(saved, "Student confirmed ticket as resolved: #" + saved.getTicketNumber());

        return toResponse(saved);
    }

    public TicketResponse assignAdmin(Long ticketId, AssignTicketRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new AppException(404, "TICKET_NOT_FOUND", "Ticket not found"));

        if (ticket.getStatus() != TicketStatus.PENDING) {
            throw new AppException(409, "INVALID_ASSIGNMENT", "Can only assign admin to PENDING tickets");
        }

        UUID targetAdminId = (request.getAdminId() != null)
                ? request.getAdminId()
                : SecurityUtils.getCurrentUserId();

        Profile admin = profileRepository.findById(targetAdminId)
                .orElseThrow(() -> new AppException(404, "ADMIN_NOT_FOUND", "Admin not found"));

        if (!"ADMIN".equals(admin.getRole())) {
            throw new AppException(400, "INVALID_ADMIN", "Target user is not an admin");
        }

        ticket.setAssignedAdminId(targetAdminId);
        Ticket saved = ticketRepository.save(ticket);

        notificationService.createNotification(
                targetAdminId,
                saved.getId(),
                "You have been assigned ticket #" + saved.getTicketNumber()
        );

        return toDetailedResponse(saved);
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
            throw new AppException(409, "INVALID_STATUS_TRANSITION", "Invalid status transition");
        }

        TicketStatus current = ticket.getStatus();
        if (!next.equals(VALID_TRANSITIONS.get(current))) {
            throw new AppException(409, "INVALID_STATUS_TRANSITION", "Invalid status transition");
        }

        if (current == TicketStatus.RESOLVED
                && next == TicketStatus.CLOSED
                && !ticket.isConfirmedResolved()) {
            throw new AppException(409, "CONFIRMATION_REQUIRED",
                    "Student must confirm resolution before the ticket can be closed");
        }

        ticket.setStatus(next);
        if (next == TicketStatus.CLOSED) {
            ticket.setArchived(true);
        }
        applyUrgency(ticket);
        Ticket saved = ticketRepository.save(ticket);
        publishTicketEvent(saved, "TICKET_STATUS_UPDATED", "ADMIN");

        notificationService.createNotification(
                saved.getUserId(),
                saved.getId(),
                "Your ticket #" + saved.getTicketNumber() + " status has been updated to " + next.name()
        );

        return toResponse(saved);
    }

    private void publishTicketEvent(Ticket ticket, String eventType, String actorRole) {
        RealtimeEventResponse event = RealtimeEventResponse.builder()
                .domain("tickets")
                .eventType(eventType)
                .entityId(String.valueOf(ticket.getId()))
                .updatedAt(ticket.getUpdatedAt())
                .actorRole(actorRole)
                .build();

        realtimeSseService.publishToUser(ticket.getUserId(), event);
        realtimeSseService.publishToAdmins(event);
    }
}
