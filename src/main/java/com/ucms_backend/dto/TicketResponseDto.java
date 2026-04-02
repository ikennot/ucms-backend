package com.ucms_backend.dto;

import com.ucms_backend.model.entity.TicketResponse;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponseDto {

    private Long id;
    private Long ticketId;
    private UUID adminId;
    private String message;
    private String ticketStatus;
    private LocalDateTime createdAt;

    public static TicketResponseDto from(TicketResponse response) {
        return TicketResponseDto.builder()
                .id(response.getId())
                .ticketId(response.getTicketId())
                .adminId(response.getAdminId())
                .message(response.getMessage())
                .ticketStatus(response.getTicketStatus())
                .createdAt(response.getCreatedAt())
                .build();
    }
}
