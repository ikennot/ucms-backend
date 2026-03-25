package com.ucms_backend.service;

import com.ucms_backend.dto.AnalyticsOverviewResponse;
import com.ucms_backend.dto.AnalyticsSummaryResponse;
import com.ucms_backend.dto.CategoryCountResponse;
import com.ucms_backend.dto.DailyTicketVolumeResponse;
import com.ucms_backend.dto.TicketResponse;
import com.ucms_backend.model.entity.Category;
import com.ucms_backend.model.entity.Ticket;
import com.ucms_backend.model.enums.TicketStatus;
import com.ucms_backend.repository.CategoryRepository;
import com.ucms_backend.repository.TicketRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;

    public AnalyticsService(TicketRepository ticketRepository, CategoryRepository categoryRepository) {
        this.ticketRepository = ticketRepository;
        this.categoryRepository = categoryRepository;
    }

    public AnalyticsSummaryResponse getSummary() {
        long totalTickets = ticketRepository.count();
        long resolvedCount = ticketRepository.countByStatusIn(List.of(TicketStatus.RESOLVED, TicketStatus.CLOSED));
        long pendingCount = ticketRepository.countByStatusIn(List.of(TicketStatus.PENDING));
        long inProgressCount = ticketRepository.countByStatusIn(List.of(TicketStatus.IN_PROGRESS));
        long unresolvedCount = pendingCount + inProgressCount;
        double resolvedPercentage = totalTickets == 0 ? 0.0 : (resolvedCount * 100.0 / totalTickets);

        return AnalyticsSummaryResponse.builder()
            .totalTickets(totalTickets)
            .resolvedCount(resolvedCount)
            .pendingCount(pendingCount)
            .inProgressCount(inProgressCount)
            .resolvedPercentage(resolvedPercentage)
            .unresolvedCount(unresolvedCount)
            .build();
    }

    public AnalyticsOverviewResponse getOverview() {
        AnalyticsSummaryResponse summary = getSummary();
        List<CategoryCountResponse> categoryBreakdown = getByCategory();

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDateTime currentStart = today.minusDays(6).atStartOfDay();
        LocalDateTime currentEnd = today.plusDays(1).atStartOfDay();
        LocalDateTime previousStart = today.minusDays(13).atStartOfDay();
        LocalDateTime previousEnd = today.minusDays(6).atStartOfDay();

        List<TicketStatus> resolvedStatuses = List.of(TicketStatus.RESOLVED, TicketStatus.CLOSED);

        long currentTotal = ticketRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(currentStart, currentEnd);
        long previousTotal = ticketRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(previousStart, previousEnd);

        long currentResolved = ticketRepository.countByStatusInAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThan(
            resolvedStatuses,
            currentStart,
            currentEnd
        );
        long previousResolved = ticketRepository.countByStatusInAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThan(
            resolvedStatuses,
            previousStart,
            previousEnd
        );

        double currentResolutionRate = currentTotal == 0 ? 0.0 : (currentResolved * 100.0 / currentTotal);
        double previousResolutionRate = previousTotal == 0 ? 0.0 : (previousResolved * 100.0 / previousTotal);
        double resolutionTrend = currentResolutionRate - previousResolutionRate;

        double currentAvgWait = computeAverageWaitHours(currentStart, currentEnd, resolvedStatuses);
        double previousAvgWait = computeAverageWaitHours(previousStart, previousEnd, resolvedStatuses);
        double averageWaitTrend = currentAvgWait - previousAvgWait;

        return AnalyticsOverviewResponse.builder()
            .resolutionRate(summary.getResolvedPercentage())
            .resolutionTrend(resolutionTrend)
            .averageWaitTimeHours(currentAvgWait)
            .averageWaitTimeTrendHours(averageWaitTrend)
            .totalTickets(summary.getTotalTickets())
            .resolvedCount(summary.getResolvedCount())
            .pendingCount(summary.getPendingCount())
            .inProgressCount(summary.getInProgressCount())
            .unresolvedCount(summary.getUnresolvedCount())
            .ticketVolumeLast7Days(buildTicketVolumeLast7Days(today))
            .categoryBreakdown(categoryBreakdown)
            .build();
    }

    public List<CategoryCountResponse> getByCategory() {
        List<Category> categories = categoryRepository.findAll();
        Map<Long, String> categoryNames = categories.stream()
            .collect(Collectors.toMap(Category::getId, Category::getName, (existing, replacement) -> existing));

        return ticketRepository.countGroupedByCategory().stream()
            .map(row -> {
                Long categoryId = ((Number) row[0]).longValue();
                long count = ((Number) row[1]).longValue();
                String categoryName = categoryNames.getOrDefault(categoryId, "Unknown");
                return CategoryCountResponse.builder()
                    .categoryId(categoryId)
                    .categoryName(categoryName)
                    .ticketCount(count)
                    .build();
            })
            .toList();
    }

    public List<TicketResponse> getUnresolved() {
        List<Ticket> tickets = ticketRepository.findByStatusInOrderByCreatedAtAsc(
            List.of(TicketStatus.PENDING, TicketStatus.IN_PROGRESS)
        );

        return tickets.stream()
            .map(ticket -> {
                String categoryName = categoryRepository.findById(ticket.getCategoryId())
                    .map(com.ucms_backend.model.entity.Category::getName)
                    .orElse(null);
                return TicketResponse.from(ticket, categoryName);
            })
            .toList();
    }

    private List<DailyTicketVolumeResponse> buildTicketVolumeLast7Days(LocalDate today) {
        List<DailyTicketVolumeResponse> volume = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();

            long count = ticketRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end);
            String day = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase(Locale.ENGLISH);

            volume.add(DailyTicketVolumeResponse.builder()
                .date(date.toString())
                .day(day)
                .ticketCount(count)
                .build());
        }

        return volume;
    }

    private double computeAverageWaitHours(LocalDateTime start, LocalDateTime end, List<TicketStatus> statuses) {
        List<Ticket> tickets = ticketRepository.findByStatusInAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThan(
            statuses,
            start,
            end
        );

        return tickets.stream()
            .filter(t -> t.getCreatedAt() != null && t.getUpdatedAt() != null)
            .mapToDouble(t -> Duration.between(t.getCreatedAt(), t.getUpdatedAt()).toMinutes() / 60.0)
            .average()
            .orElse(0.0);
    }
}
