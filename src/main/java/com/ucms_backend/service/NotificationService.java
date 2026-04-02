package com.ucms_backend.service;

import com.ucms_backend.dto.NotificationResponse;
import com.ucms_backend.dto.RealtimeEventResponse;
import com.ucms_backend.exception.AppException;
import com.ucms_backend.model.entity.Notification;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import com.ucms_backend.repository.NotificationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final RealtimeSseService realtimeSseService;

    public NotificationService(NotificationRepository notificationRepository, RealtimeSseService realtimeSseService) {
        this.notificationRepository = notificationRepository;
        this.realtimeSseService = realtimeSseService;
    }

    public List<NotificationResponse> getNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public NotificationResponse markAsRead(Long id, UUID userId) {
        Notification notification = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AppException(404, "NOTIFICATION_NOT_FOUND", "Notification not found"));

        notification.setRead(true);
        Notification saved = notificationRepository.save(notification);
        publishNotificationEvent(userId, saved.getId(), "NOTIFICATION_READ", "STUDENT");
        return NotificationResponse.from(saved);
    }

    public void markAllAsRead(UUID userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        notifications.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notifications);
        publishNotificationEvent(userId, null, "NOTIFICATIONS_READ_ALL", "STUDENT");
    }

    public void createNotification(UUID userId, Long ticketId, String message) {
        Notification notification = Notification.builder()
                .userId(userId)
                .ticketId(ticketId)
                .message(message)
                .build();
        Notification saved = notificationRepository.save(notification);
        publishNotificationEvent(userId, saved.getId(), "NOTIFICATION_CREATED", "SYSTEM");
    }

    private void publishNotificationEvent(UUID userId, Long notificationId, String eventType, String actorRole) {
        realtimeSseService.publishToUser(userId, RealtimeEventResponse.builder()
                .domain("notifications")
                .eventType(eventType)
                .entityId(notificationId != null ? String.valueOf(notificationId) : userId.toString())
                .updatedAt(LocalDateTime.now(ZoneOffset.UTC))
                .actorRole(actorRole)
                .build());
    }
}
