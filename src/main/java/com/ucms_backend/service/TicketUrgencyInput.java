package com.ucms_backend.service;

import com.ucms_backend.model.enums.TicketStatus;
import java.time.LocalDateTime;

public record TicketUrgencyInput(
        String title,
        String description,
        String categoryName,
        TicketStatus status,
        LocalDateTime createdAt,
        LocalDateTime lastAdminResponseAt,
        Long adminResponseCount,
        Long attachmentCount,
        Integer followUpCount
) {
}
