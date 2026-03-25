package com.ucms_backend.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsOverviewResponse {
    private double resolutionRate;
    private double resolutionTrend;
    private double averageWaitTimeHours;
    private double averageWaitTimeTrendHours;
    private long totalTickets;
    private long resolvedCount;
    private long pendingCount;
    private long inProgressCount;
    private long unresolvedCount;
    private List<DailyTicketVolumeResponse> ticketVolumeLast7Days;
    private List<CategoryCountResponse> categoryBreakdown;
}
