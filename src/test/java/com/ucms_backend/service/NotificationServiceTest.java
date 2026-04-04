package com.ucms_backend.service;

import com.ucms_backend.dto.NotificationResponse;
import com.ucms_backend.exception.AppException;
import com.ucms_backend.model.entity.Notification;
import com.ucms_backend.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private RealtimeSseService realtimeSseService;

    @InjectMocks
    private NotificationService notificationService;

    // -------------------------------------------------------------------------
    // getNotifications
    // -------------------------------------------------------------------------

    @Test
    void getNotifications_returnsListOrderedByCreatedAtDesc() {
        UUID userId = UUID.randomUUID();
        Notification older = Notification.builder()
                .id(1L)
                .userId(userId)
                .ticketId(10L)
                .message("Old notification")
                .isRead(false)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
        Notification newer = Notification.builder()
                .id(2L)
                .userId(userId)
                .ticketId(10L)
                .message("New notification")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(newer, older));

        List<NotificationResponse> result = notificationService.getNotifications(userId);

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getId());
        assertEquals(1L, result.get(1).getId());
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(userId);
    }

    // -------------------------------------------------------------------------
    // markAsRead — own notification
    // -------------------------------------------------------------------------

    @Test
    void markAsRead_ownNotification_setsIsReadTrue() {
        UUID userId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(5L)
                .userId(userId)
                .ticketId(10L)
                .message("You have a new response")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.findByIdAndUserId(5L, userId))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        NotificationResponse result = notificationService.markAsRead(5L, userId);

        assertTrue(result.isRead());
        verify(notificationRepository).save(notification);
    }

    // -------------------------------------------------------------------------
    // markAsRead — another student's notification → 404 (no existence leak)
    // -------------------------------------------------------------------------

    @Test
    void markAsRead_otherStudentNotification_throwsNotificationNotFound() {
        UUID userId = UUID.randomUUID();

        when(notificationRepository.findByIdAndUserId(99L, userId))
                .thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> notificationService.markAsRead(99L, userId));

        assertEquals(404, exception.getStatus());
        assertEquals("NOTIFICATION_NOT_FOUND", exception.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // markAllAsRead
    // -------------------------------------------------------------------------

    @Test
    void markAllAsRead_setsAllIsReadTrue() {
        UUID userId = UUID.randomUUID();
        Notification n1 = Notification.builder()
                .id(1L)
                .userId(userId)
                .ticketId(10L)
                .message("First")
                .isRead(false)
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();
        Notification n2 = Notification.builder()
                .id(2L)
                .userId(userId)
                .ticketId(11L)
                .message("Second")
                .isRead(false)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(n2, n1));

        notificationService.markAllAsRead(userId);

        assertTrue(n1.isRead());
        assertTrue(n2.isRead());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        assertEquals(2, captor.getValue().size());
        captor.getValue().forEach(n -> assertTrue(n.isRead()));
    }
}
