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
public class SyncResponse<T> {

    private T items;
    private boolean fullRefresh;
    private LocalDateTime serverTime;
    private LocalDateTime nextSince;
}
