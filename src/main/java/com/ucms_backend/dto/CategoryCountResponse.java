package com.ucms_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryCountResponse {
    private Long categoryId;
    private String categoryName;
    private long ticketCount;
}
