package com.ucms_backend.controller;

import com.ucms_backend.dto.ApiResponse;
import com.ucms_backend.dto.TicketResponse;
import com.ucms_backend.dto.UrgencyOverrideRequest;
import com.ucms_backend.service.TicketUrgencyRescoreScheduler;
import com.ucms_backend.service.TicketUrgencyScoringService;
import com.ucms_backend.service.TicketService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ai")
public class AiAdminController {

    private final TicketUrgencyScoringService ticketUrgencyScoringService;
    private final TicketUrgencyRescoreScheduler ticketUrgencyRescoreScheduler;
    private final TicketService ticketService;

    public AiAdminController(
            TicketUrgencyScoringService ticketUrgencyScoringService,
            TicketUrgencyRescoreScheduler ticketUrgencyRescoreScheduler,
            TicketService ticketService
    ) {
        this.ticketUrgencyScoringService = ticketUrgencyScoringService;
        this.ticketUrgencyRescoreScheduler = ticketUrgencyRescoreScheduler;
        this.ticketService = ticketService;
    }

    @GetMapping("/gemini/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> geminiHealth() {
        Map<String, Object> health = ticketUrgencyScoringService.healthCheck();
        return ResponseEntity.ok(ApiResponse.ok("Gemini health check completed", health));
    }

    @PostMapping("/gemini/rescore/open")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rescoreOpenTickets(
            @RequestParam(name = "limit", defaultValue = "100") int limit
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        int updated = ticketUrgencyRescoreScheduler.rescoreOpenTicketsNow(safeLimit);
        Map<String, Object> data = Map.of(
                "updated", updated,
                "requestedLimit", safeLimit
        );
        return ResponseEntity.ok(ApiResponse.ok("Open tickets re-scored", data));
    }

    @PatchMapping("/tickets/{ticketId}/urgency-override")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TicketResponse>> overrideTicketUrgency(
            @PathVariable Long ticketId,
            @Valid @RequestBody UrgencyOverrideRequest request
    ) {
        TicketResponse response = ticketService.overrideUrgency(ticketId, request);
        return ResponseEntity.ok(ApiResponse.ok("Ticket urgency overridden", response));
    }
}
