package com.ucms_backend.service;

import com.ucms_backend.dto.NotificationResponse;
import com.ucms_backend.exception.AppException;
import com.ucms_backend.model.entity.Notification;
import com.ucms_backend.repository.NotificationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
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
        return NotificationResponse.from(saved);
    }

    public void markAllAsRead(UUID userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        notifications.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    public void createNotification(UUID userId, Long ticketId, String message) {
        Notification notification = Notification.builder()
                .userId(userId)
                .ticketId(ticketId)
                .message(message)
                .build();
        notificationRepository.save(notification);
    }
}
