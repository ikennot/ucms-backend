package com.ucms_backend.service;

import com.ucms_backend.dto.AnalyticsSummaryResponse;
import com.ucms_backend.dto.CategoryCountResponse;
import com.ucms_backend.dto.TicketResponse;
import com.ucms_backend.model.entity.Category;
import com.ucms_backend.model.entity.Ticket;
import com.ucms_backend.model.enums.TicketStatus;
import com.ucms_backend.repository.CategoryRepository;
import com.ucms_backend.repository.TicketRepository;
import java.util.List;
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
        long unresolvedCount = ticketRepository.countByStatusIn(List.of(TicketStatus.PENDING, TicketStatus.IN_PROGRESS));
        double resolvedPercentage = totalTickets == 0 ? 0.0 : (resolvedCount * 100.0 / totalTickets);

        return AnalyticsSummaryResponse.builder()
            .totalTickets(totalTickets)
            .resolvedCount(resolvedCount)
            .resolvedPercentage(resolvedPercentage)
            .unresolvedCount(unresolvedCount)
            .build();
    }

    public List<CategoryCountResponse> getByCategory() {
        List<Category> categories = categoryRepository.findAll();
        Map<Long, String> categoryNames = categories.stream()
            .collect(Collectors.toMap(Category::getId, Category::getName, (existing, replacement) -> existing));

        return ticketRepository.countGroupedByCategory().stream()
            .map(row -> {
                Long categoryId = (Long) row[0];
                long count = (Long) row[1];
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
}
