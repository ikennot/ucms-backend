package com.ucms_backend.controller;

import com.ucms_backend.dto.AnalyticsOverviewResponse;
import com.ucms_backend.dto.ApiResponse;
import com.ucms_backend.dto.NotificationResponse;
import com.ucms_backend.dto.ProfileResponse;
import com.ucms_backend.dto.SyncResponse;
import com.ucms_backend.dto.TicketResponse;
import com.ucms_backend.service.SyncService;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ResponseEntity<ApiResponse<SyncResponse<ProfileResponse>>> syncProfile(
            @RequestParam(required = false) Instant since
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Profile sync retrieved", syncService.syncProfile(since)));
    }

    @GetMapping("/tickets")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ResponseEntity<ApiResponse<SyncResponse<List<TicketResponse>>>> syncTickets(
            @RequestParam(required = false) Instant since
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Ticket sync retrieved", syncService.syncTickets(since)));
    }

    @GetMapping("/notifications")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<SyncResponse<List<NotificationResponse>>>> syncNotifications(
            @RequestParam(required = false) Instant since
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Notification sync retrieved", syncService.syncNotifications(since)));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SyncResponse<AnalyticsOverviewResponse>>> syncAnalytics(
            @RequestParam(required = false) Instant since
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Analytics sync retrieved", syncService.syncAnalytics(since)));
    }
}
