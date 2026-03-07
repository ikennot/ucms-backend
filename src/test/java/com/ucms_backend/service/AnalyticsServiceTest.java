package com.ucms_backend.service;

import com.ucms_backend.dto.AnalyticsSummaryResponse;
import com.ucms_backend.dto.CategoryCountResponse;
import com.ucms_backend.dto.TicketResponse;
import com.ucms_backend.model.entity.Category;
import com.ucms_backend.model.entity.Ticket;
import com.ucms_backend.model.enums.TicketStatus;
import com.ucms_backend.repository.CategoryRepository;
import com.ucms_backend.repository.TicketRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void getSummary_emptyDatabase_returnsZeroAndZeroPercent() {
        when(ticketRepository.count()).thenReturn(0L);
        when(ticketRepository.countByStatusIn(anyList())).thenReturn(0L);

        AnalyticsSummaryResponse response = analyticsService.getSummary();

        assertEquals(0L, response.getTotalTickets());
        assertEquals(0L, response.getResolvedCount());
        assertEquals(0.0, response.getResolvedPercentage());
        assertEquals(0L, response.getUnresolvedCount());
    }

    @Test
    void getSummary_mixedStatuses_computesCorrectPercentage() {
        when(ticketRepository.count()).thenReturn(4L);
        when(ticketRepository.countByStatusIn(List.of(TicketStatus.RESOLVED, TicketStatus.CLOSED))).thenReturn(3L);
        when(ticketRepository.countByStatusIn(List.of(TicketStatus.PENDING, TicketStatus.IN_PROGRESS))).thenReturn(1L);

        AnalyticsSummaryResponse response = analyticsService.getSummary();

        assertEquals(4L, response.getTotalTickets());
        assertEquals(3L, response.getResolvedCount());
        assertEquals(75.0, response.getResolvedPercentage());
        assertEquals(1L, response.getUnresolvedCount());
    }

    @Test
    void getByCategory_multipleCategories_returnsCountPerCategory() {
        List<Category> categories = List.of(
            new Category(1L, "Academic-related"),
            new Category(2L, "Facility Issue")
        );
        List<Object[]> grouped = List.of(
            new Object[] {1L, 2L},
            new Object[] {2L, 1L}
        );

        when(categoryRepository.findAll()).thenReturn(categories);
        when(ticketRepository.countGroupedByCategory()).thenReturn(grouped);

        List<CategoryCountResponse> response = analyticsService.getByCategory();

        Map<Long, CategoryCountResponse> byId = response.stream()
            .collect(Collectors.toMap(CategoryCountResponse::getCategoryId, item -> item));

        assertEquals(2, response.size());
        assertEquals("Academic-related", byId.get(1L).getCategoryName());
        assertEquals(2L, byId.get(1L).getTicketCount());
        assertEquals("Facility Issue", byId.get(2L).getCategoryName());
        assertEquals(1L, byId.get(2L).getTicketCount());
    }

    @Test
    void getByCategory_noTickets_returnsEmptyList() {
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(ticketRepository.countGroupedByCategory()).thenReturn(List.of());

        List<CategoryCountResponse> response = analyticsService.getByCategory();

        assertTrue(response.isEmpty());
    }

    @Test
    void getUnresolved_none_returnsEmptyList() {
        when(ticketRepository.findByStatusInOrderByCreatedAtAsc(List.of(TicketStatus.PENDING, TicketStatus.IN_PROGRESS)))
            .thenReturn(List.of());

        List<TicketResponse> response = analyticsService.getUnresolved();

        assertTrue(response.isEmpty());
    }

    @Test
    void getUnresolved_some_returnsOrderedList() {
        Ticket first = Ticket.builder()
            .id(1L)
            .userId(UUID.randomUUID())
            .categoryId(10L)
            .ticketNumber("TKT-1")
            .title("Title 1")
            .description("Desc 1")
            .status(TicketStatus.PENDING)
            .createdAt(LocalDateTime.parse("2026-03-01T10:00:00"))
            .updatedAt(LocalDateTime.parse("2026-03-01T10:00:00"))
            .build();
        Ticket second = Ticket.builder()
            .id(2L)
            .userId(UUID.randomUUID())
            .categoryId(11L)
            .ticketNumber("TKT-2")
            .title("Title 2")
            .description("Desc 2")
            .status(TicketStatus.IN_PROGRESS)
            .createdAt(LocalDateTime.parse("2026-03-02T10:00:00"))
            .updatedAt(LocalDateTime.parse("2026-03-02T10:00:00"))
            .build();

        when(ticketRepository.findByStatusInOrderByCreatedAtAsc(List.of(TicketStatus.PENDING, TicketStatus.IN_PROGRESS)))
            .thenReturn(List.of(first, second));

        List<TicketResponse> response = analyticsService.getUnresolved();

        assertEquals(2, response.size());
        assertEquals(1L, response.get(0).getId());
        assertEquals("PENDING", response.get(0).getStatus());
        assertEquals(2L, response.get(1).getId());
        assertEquals("IN_PROGRESS", response.get(1).getStatus());
    }
}
