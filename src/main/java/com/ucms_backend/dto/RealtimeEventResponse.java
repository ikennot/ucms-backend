package com.ucms_backend.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeEventResponse {
    private String domain;
    private String eventType;
    private String entityId;
    private LocalDateTime updatedAt;
    private String actorRole;
}
