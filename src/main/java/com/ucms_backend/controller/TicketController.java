package com.ucms_backend.controller;

import com.ucms_backend.dto.ApiResponse;
import com.ucms_backend.dto.CreateTicketRequest;
import com.ucms_backend.dto.TicketResponse;
import com.ucms_backend.dto.UpdateStatusRequest;
import com.ucms_backend.service.TicketService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<TicketResponse>> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        UUID userId = getUserId();
        TicketResponse ticketResponse = ticketService.createTicket(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Ticket created", ticketResponse));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId
    ) {
        UUID userId = getUserId();
        String role = getRole();
        List<TicketResponse> tickets = ticketService.getTickets(userId, role, status, categoryId);
        return ResponseEntity.ok(ApiResponse.ok("Tickets retrieved", tickets));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicketById(@PathVariable Long id) {
        UUID userId = getUserId();
        String role = getRole();
        TicketResponse ticketResponse = ticketService.getTicketById(id, userId, role);
        return ResponseEntity.ok(ApiResponse.ok("Ticket retrieved", ticketResponse));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TicketResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        TicketResponse ticketResponse = ticketService.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Status updated", ticketResponse));
    }

    @PatchMapping("/{id}/confirm-resolved")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<TicketResponse>> confirmResolved(@PathVariable Long id) {
        UUID userId = getUserId();
        TicketResponse ticketResponse = ticketService.confirmResolved(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Ticket confirmed as resolved", ticketResponse));
    }

    private UUID getUserId() {
        return (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private String getRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }
}
