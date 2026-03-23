package com.ucms_backend.service;

import java.time.LocalDateTime;

public record TicketUrgencyEvaluation(
        boolean urgent,
        int score,
        String priorityLevel,
        double confidence,
        String reason,
        String signals,
        LocalDateTime updatedAt,
        boolean needsHumanReview
) {
}
