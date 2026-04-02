package com.ucms_backend.service;

import com.ucms_backend.model.entity.Category;
import com.ucms_backend.model.entity.Ticket;
import com.ucms_backend.model.enums.TicketStatus;
import com.ucms_backend.repository.CategoryRepository;
import com.ucms_backend.repository.TicketRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketUrgencyRescoreSchedulerTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TicketUrgencyScoringService ticketUrgencyScoringService;

    @Test
    void rescoreOpenTickets_updatesAndSavesBatch() {
        TicketUrgencyRescoreScheduler scheduler = new TicketUrgencyRescoreScheduler(
                ticketRepository,
                categoryRepository,
                ticketUrgencyScoringService,
                true,
                50,
                30
        );

        Ticket ticket = Ticket.builder()
                .id(10L)
                .userId(UUID.randomUUID())
                .categoryId(1L)
                .ticketNumber("TKT-1")
                .title("Need urgent help")
                .description("Issue pending for 2 days")
                .status(TicketStatus.PENDING)
                .createdAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(2))
                .updatedAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(1))
                .build();

        when(ticketRepository.findForUrgencyRefresh(any(), any(), ArgumentMatchers.any(Pageable.class)))
                .thenReturn(List.of(ticket));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(new Category(1L, "Academic")));
        when(ticketUrgencyScoringService.evaluate(eq(ticket), eq("Academic"))).thenReturn(
                new TicketUrgencyEvaluation(
                        true,
                        72,
                        "HIGH",
                        0.84,
                        "Re-scored by scheduler",
                        "Aging pending concern; no admin response",
                        LocalDateTime.now(ZoneOffset.UTC),
                        false
                )
        );

        scheduler.rescoreOpenTickets();

        assertEquals(72, ticket.getUrgencyScore());
        assertEquals("HIGH", ticket.getUrgencyLabel());
        verify(ticketRepository).saveAll(List.of(ticket));
    }

    @Test
    void rescoreOpenTickets_disabled_doesNothing() {
        TicketUrgencyRescoreScheduler scheduler = new TicketUrgencyRescoreScheduler(
                ticketRepository,
                categoryRepository,
                ticketUrgencyScoringService,
                false,
                50,
                30
        );

        scheduler.rescoreOpenTickets();

        verify(ticketRepository, never()).findForUrgencyRefresh(any(), any(), any(Pageable.class));
        verify(ticketRepository, never()).saveAll(any());
    }

    @Test
    void rescoreOpenTicketsNow_forceRefreshesOpenTickets() {
        TicketUrgencyRescoreScheduler scheduler = new TicketUrgencyRescoreScheduler(
                ticketRepository,
                categoryRepository,
                ticketUrgencyScoringService,
                false,
                50,
                30
        );

        Ticket ticket = Ticket.builder()
                .id(11L)
                .userId(UUID.randomUUID())
                .categoryId(1L)
                .ticketNumber("TKT-2")
                .title("Bullying concern")
                .description("Student reports repeated bullying")
                .status(TicketStatus.PENDING)
                .createdAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(1))
                .updatedAt(LocalDateTime.now(ZoneOffset.UTC).minusHours(10))
                .build();

        when(ticketRepository.findByStatusInOrderByCreatedAtAsc(any(), eq(PageRequest.of(0, 20))))
                .thenReturn(List.of(ticket));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(new Category(1L, "Academic")));
        when(ticketUrgencyScoringService.evaluate(eq(ticket), eq("Academic"))).thenReturn(
                new TicketUrgencyEvaluation(
                        true,
                        90,
                        "CRITICAL",
                        0.9,
                        "Bullying signal detected",
                        "Bullying phrase detected",
                        LocalDateTime.now(ZoneOffset.UTC),
                        false
                )
        );

        int updated = scheduler.rescoreOpenTicketsNow(20);

        assertEquals(1, updated);
        verify(ticketRepository).findByStatusInOrderByCreatedAtAsc(any(), eq(PageRequest.of(0, 20)));
        verify(ticketRepository).saveAll(List.of(ticket));
    }
}
