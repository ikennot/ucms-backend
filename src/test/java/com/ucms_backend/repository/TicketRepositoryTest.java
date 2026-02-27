package com.ucms_backend.repository;

import com.ucms_backend.model.entity.Ticket;
import com.ucms_backend.model.enums.TicketStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketRepositoryTest {

    @Mock
    private TicketRepository ticketRepository;

    @Test
    void findByUserId_returnsMatchingTickets() {
        UUID userId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .ticketNumber("TKT-20260227-0001")
                .title("Cannot submit grades")
                .status(TicketStatus.PENDING)
                .confirmedResolved(false)
                .build();

        when(ticketRepository.findByUserId(userId)).thenReturn(List.of(ticket));

        List<Ticket> result = ticketRepository.findByUserId(userId);

        assertEquals(1, result.size());
        assertEquals(userId, result.get(0).getUserId());
        assertEquals("TKT-20260227-0001", result.get(0).getTicketNumber());
    }

    @Test
    void findByUserId_noTickets_returnsEmpty() {
        UUID userId = UUID.randomUUID();
        when(ticketRepository.findByUserId(userId)).thenReturn(List.of());

        List<Ticket> result = ticketRepository.findByUserId(userId);

        assertEquals(0, result.size());
    }

    @Test
    void findAll_withSpecification_returnsFilteredTickets() {
        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .ticketNumber("TKT-20260227-0001")
                .title("Cannot submit grades")
                .status(TicketStatus.PENDING)
                .confirmedResolved(false)
                .build();

        Specification<Ticket> spec = (root, query, cb) -> cb.equal(root.get("status"), TicketStatus.PENDING);
        when(ticketRepository.findAll(spec)).thenReturn(List.of(ticket));

        List<Ticket> result = ticketRepository.findAll(spec);

        assertEquals(1, result.size());
        assertEquals(TicketStatus.PENDING, result.get(0).getStatus());
    }
}
