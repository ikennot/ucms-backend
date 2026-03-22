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
    private String studentName;
    private String studentId;
    private String studentCourse;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TicketResponse from(Ticket ticket, String categoryName) {
        return from(ticket, categoryName, null);
    }

    public static TicketResponse from(Ticket ticket, String categoryName, Profile profile) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(ticket.getStatus().name())
                .confirmedResolved(ticket.isConfirmedResolved())
                .categoryId(ticket.getCategoryId())
                .categoryName(categoryName)
                .studentName(profile != null ? profile.getName() : null)
                .studentId(profile != null ? profile.getStudentId() : null)
                .studentCourse(profile != null ? profile.getCourse() : null)
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }
}
