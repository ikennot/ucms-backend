package com.ucms_backend.service;

import com.ucms_backend.dto.AnalyticsOverviewResponse;
import com.ucms_backend.dto.NotificationResponse;
import com.ucms_backend.dto.ProfileResponse;
import com.ucms_backend.dto.SyncResponse;
import com.ucms_backend.dto.TicketResponse;
import com.ucms_backend.security.SecurityUtils;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SyncService {

    private final ProfileService profileService;
    private final TicketService ticketService;
    private final NotificationService notificationService;
    private final AnalyticsService analyticsService;

    public SyncService(
            ProfileService profileService,
            TicketService ticketService,
            NotificationService notificationService,
            AnalyticsService analyticsService
    ) {
        this.profileService = profileService;
        this.ticketService = ticketService;
        this.notificationService = notificationService;
        this.analyticsService = analyticsService;
    }

    public SyncResponse<ProfileResponse> syncProfile(Instant since) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ProfileResponse profile = profileService.getMyProfile(userId);
        LocalDateTime now = utcNow();

        return SyncResponse.<ProfileResponse>builder()
                .items(profile)
                .fullRefresh(true)
                .serverTime(now)
                .nextSince(now)
                .build();
    }

    public SyncResponse<List<TicketResponse>> syncTickets(Instant since) {
        List<TicketResponse> tickets = ticketService.getTicketsSince(since, false);
        LocalDateTime now = utcNow();

        return SyncResponse.<List<TicketResponse>>builder()
                .items(tickets)
                .fullRefresh(since == null)
                .serverTime(now)
                .nextSince(now)
                .build();
    }

    public SyncResponse<List<NotificationResponse>> syncNotifications(Instant since) {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<NotificationResponse> notifications = notificationService.getNotifications(userId);
        LocalDateTime now = utcNow();

        return SyncResponse.<List<NotificationResponse>>builder()
                .items(notifications)
                .fullRefresh(true)
                .serverTime(now)
                .nextSince(now)
                .build();
    }

    public SyncResponse<AnalyticsOverviewResponse> syncAnalytics(Instant since) {
        AnalyticsOverviewResponse overview = analyticsService.getOverview();
        LocalDateTime now = utcNow();

        return SyncResponse.<AnalyticsOverviewResponse>builder()
                .items(overview)
                .fullRefresh(true)
                .serverTime(now)
                .nextSince(now)
                .build();
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
