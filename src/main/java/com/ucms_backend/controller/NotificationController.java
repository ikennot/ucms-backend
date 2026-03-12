package com.ucms_backend.controller;

import com.ucms_backend.dto.ApiResponse;
import com.ucms_backend.dto.NotificationResponse;
import com.ucms_backend.service.NotificationService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications() {
        List<NotificationResponse> notifications = notificationService.getNotifications(getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Notifications retrieved", notifications));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(@PathVariable Long id) {
        NotificationResponse notification = notificationService.markAsRead(id, getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Notification marked as read", notification));
    }

    @PatchMapping("/read-all")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        notificationService.markAllAsRead(getUserId());
        return ResponseEntity.ok(ApiResponse.ok("All notifications marked as read"));
    }

    private UUID getUserId() {
        return (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
