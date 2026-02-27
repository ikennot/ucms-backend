package com.ucms_backend.service;

import com.ucms_backend.repository.TicketRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketNumberGeneratorTest {

    @Mock
    private TicketRepository ticketRepository;

    private TicketNumberGenerator generator;

    private static final Clock FIXED_CLOCK = Clock.fixed(
            LocalDate.of(2026, 2, 27).atStartOfDay(ZoneOffset.UTC).toInstant(),
            ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        generator = new TicketNumberGenerator(ticketRepository, FIXED_CLOCK);
    }

    @Test
    void generate_firstTicketOfDay_returnsPaddedOne() {
        when(ticketRepository.countByTicketNumberStartingWith("TKT-20260227-")).thenReturn(0L);

        assertEquals("TKT-20260227-0001", generator.generate());
    }

    @Test
    void generate_tenthTicketOfDay_returnsPaddedTen() {
        when(ticketRepository.countByTicketNumberStartingWith("TKT-20260227-")).thenReturn(9L);

        assertEquals("TKT-20260227-0010", generator.generate());
    }

    @Test
    void generate_usesCurrentDate() {
        Clock differentDay = Clock.fixed(
                LocalDate.of(2026, 3, 1).atStartOfDay(ZoneOffset.UTC).toInstant(),
                ZoneOffset.UTC);
        TicketNumberGenerator gen = new TicketNumberGenerator(ticketRepository, differentDay);

        when(ticketRepository.countByTicketNumberStartingWith("TKT-20260301-")).thenReturn(0L);

        assertEquals("TKT-20260301-0001", gen.generate());
    }
}
