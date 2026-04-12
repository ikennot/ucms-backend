package com.ucms_backend.dto;

import com.ucms_backend.model.entity.Category;
import com.ucms_backend.model.entity.Profile;
import com.ucms_backend.model.entity.Ticket;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {

    private Long id;
    private String ticketNumber;
    private String title;
    private String description;
    private String status;
    private boolean confirmedResolved;
    private Long categoryId;
    private String categoryName;
    private boolean hasAdminResponse;
    private boolean urgent;
    private Integer urgencyScore;
    private String urgencyLabel;
    private String urgencyReason;
    private String urgencySignals;
    private Double urgencyConfidence;
    private LocalDateTime urgencyUpdatedAt;
    private boolean urgencyOverridden;
    private String urgencyOverrideReason;
    private java.util.UUID assignedAdminId;
    private String assignedAdminName;
    private String studentName;
    private String studentId;
    private String studentCourse;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TicketResponse from(Ticket ticket, String categoryName) {
        return from(ticket, categoryName, null, false);
    }

    public static TicketResponse from(Ticket ticket, String categoryName, Profile profile) {
        return from(ticket, categoryName, profile, false);
    }

    public static TicketResponse from(Ticket ticket, String categoryName, Profile profile, boolean hasAdminResponse) {
        return from(ticket, categoryName, profile, hasAdminResponse, null);
    }

    public static TicketResponse from(Ticket ticket, String categoryName, Profile profile, boolean hasAdminResponse, String assignedAdminName) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(ticket.getStatus().name())
                .confirmedResolved(ticket.isConfirmedResolved())
                .categoryId(ticket.getCategoryId())
                .categoryName(categoryName)
                .hasAdminResponse(hasAdminResponse)
                .urgent(isUrgent(ticket))
                .urgencyScore(ticket.getUrgencyScore())
                .urgencyLabel(ticket.getUrgencyLabel())
                .urgencyReason(ticket.getUrgencyReason())
                .urgencySignals(ticket.getUrgencySignals())
                .urgencyConfidence(ticket.getUrgencyConfidence())
                .urgencyUpdatedAt(ticket.getUrgencyUpdatedAt())
                .urgencyOverridden(ticket.isUrgencyOverridden())
                .urgencyOverrideReason(ticket.getUrgencyOverrideReason())
                .assignedAdminId(ticket.getAssignedAdminId())
                .assignedAdminName(assignedAdminName)
                .studentName(profile != null ? profile.getName() : null)
                .studentId(profile != null ? profile.getStudentId() : null)
                .studentCourse(profile != null ? profile.getCourse() : null)
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }

    private static boolean isUrgent(Ticket ticket) {
        if (ticket.getUrgencyScore() != null && ticket.getUrgencyScore() >= 60) {
            return true;
        }
        if (ticket.getUrgencyLabel() == null) {
            return false;
        }
        String label = ticket.getUrgencyLabel().trim().toUpperCase();
        return "HIGH".equals(label) || "CRITICAL".equals(label);
    }
}
