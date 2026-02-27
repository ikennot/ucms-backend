package com.ucms_backend.service;

import com.ucms_backend.repository.TicketRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

@Service
public class TicketNumberGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final TicketRepository ticketRepository;
    private final Clock clock;

    public TicketNumberGenerator(TicketRepository ticketRepository, Clock clock) {
        this.ticketRepository = ticketRepository;
        this.clock = clock;
    }

    public String generate() {
        String prefix = "TKT-" + LocalDate.now(clock).format(DATE_FORMATTER) + "-";
        long count = ticketRepository.countByTicketNumberStartingWith(prefix);
        return String.format("%s%04d", prefix, count + 1);
    }
}
