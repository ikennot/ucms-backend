package com.ucms_backend.controller;

import com.ucms_backend.dto.AnalyticsOverviewResponse;
import com.ucms_backend.dto.AnalyticsSummaryResponse;
import com.ucms_backend.dto.ApiResponse;
import com.ucms_backend.dto.CategoryCountResponse;
import com.ucms_backend.dto.TicketResponse;
import com.ucms_backend.service.AnalyticsService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AnalyticsSummaryResponse>> getSummary() {
        AnalyticsSummaryResponse summary = analyticsService.getSummary();
        return ResponseEntity.ok(ApiResponse.ok("Analytics summary retrieved", summary));
    }

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AnalyticsOverviewResponse>> getOverview() {
        AnalyticsOverviewResponse overview = analyticsService.getOverview();
        return ResponseEntity.ok(ApiResponse.ok("Analytics overview retrieved", overview));
    }

    @GetMapping("/by-category")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<CategoryCountResponse>>> getByCategory() {
        List<CategoryCountResponse> results = analyticsService.getByCategory();
        return ResponseEntity.ok(ApiResponse.ok("Analytics by category retrieved", results));
    }

    @GetMapping("/unresolved")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getUnresolved() {
        List<TicketResponse> tickets = analyticsService.getUnresolved();
        return ResponseEntity.ok(ApiResponse.ok("Unresolved tickets retrieved", tickets));
    }
}
