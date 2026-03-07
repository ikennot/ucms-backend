package com.ucms_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSummaryResponse {
    private long totalTickets;
    private long resolvedCount;
    private double resolvedPercentage;
    private long unresolvedCount;
}
