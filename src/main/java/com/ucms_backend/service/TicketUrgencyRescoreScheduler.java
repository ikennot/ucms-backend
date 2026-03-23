package com.ucms_backend.service;

import com.ucms_backend.model.entity.Category;
import com.ucms_backend.model.entity.Ticket;
import com.ucms_backend.model.enums.TicketStatus;
import com.ucms_backend.repository.CategoryRepository;
import com.ucms_backend.repository.TicketRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

@Service
public class TicketUrgencyRescoreScheduler {

    private static final Logger log = LoggerFactory.getLogger(TicketUrgencyRescoreScheduler.class);
    private static final List<TicketStatus> OPEN_STATUSES = List.of(TicketStatus.PENDING, TicketStatus.IN_PROGRESS);

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final TicketUrgencyScoringService ticketUrgencyScoringService;
    private final boolean enabled;
    private final int batchSize;
    private final int minRefreshIntervalMinutes;

    public TicketUrgencyRescoreScheduler(
            TicketRepository ticketRepository,
            CategoryRepository categoryRepository,
            TicketUrgencyScoringService ticketUrgencyScoringService,
            @Value("${urgency-rescore.enabled:true}") boolean enabled,
            @Value("${urgency-rescore.batch-size:50}") int batchSize,
            @Value("${urgency-rescore.min-refresh-minutes:30}") int minRefreshIntervalMinutes
    ) {
        this.ticketRepository = ticketRepository;
        this.categoryRepository = categoryRepository;
        this.ticketUrgencyScoringService = ticketUrgencyScoringService;
        this.enabled = enabled;
        this.batchSize = Math.max(1, batchSize);
        this.minRefreshIntervalMinutes = Math.max(5, minRefreshIntervalMinutes);
    }

    @Scheduled(
            fixedDelayString = "${urgency-rescore.fixed-delay-ms:1800000}",
            initialDelayString = "${urgency-rescore.initial-delay-ms:120000}"
    )
    @Transactional
    public void rescoreOpenTickets() {
        if (!enabled) {
            return;
        }

        int refreshed = refreshOpenTickets(batchSize, false);
        if (refreshed > 0) {
            log.info("Urgency scheduler refreshed {} tickets", refreshed);
        }
    }

    @Transactional
    public int rescoreOpenTicketsNow(int limit) {
        int safeLimit = Math.max(1, limit);
        int refreshed = refreshOpenTickets(safeLimit, true);
        if (refreshed > 0) {
            log.info("Urgency manual re-score refreshed {} tickets", refreshed);
        }
        return refreshed;
    }

    private int refreshOpenTickets(int limit, boolean forceRefreshAll) {
        int safeLimit = Math.max(1, limit);

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<Ticket> candidates;
        if (forceRefreshAll) {
            candidates = ticketRepository.findByStatusInOrderByCreatedAtAsc(
                    OPEN_STATUSES,
                    PageRequest.of(0, safeLimit)
            );
        } else {
            LocalDateTime cutoff = now.minusMinutes(minRefreshIntervalMinutes);
            candidates = ticketRepository.findForUrgencyRefresh(
                    OPEN_STATUSES,
                    cutoff,
                    PageRequest.of(0, safeLimit)
            );
        }

        if (candidates.isEmpty()) {
            return 0;
        }

        int updatedCount = 0;
        for (Ticket ticket : candidates) {
            if (ticket.isUrgencyOverridden()) {
                continue;
            }
            try {
                String categoryName = categoryRepository.findById(ticket.getCategoryId())
                        .map(Category::getName)
                        .orElse(null);

                TicketUrgencyEvaluation evaluation = ticketUrgencyScoringService.evaluate(ticket, categoryName);
                ticket.setUrgencyScore(evaluation.score());
                ticket.setUrgencyLabel(evaluation.priorityLevel());
                ticket.setUrgencyReason(evaluation.reason());
                ticket.setUrgencySignals(evaluation.signals());
                ticket.setUrgencyConfidence(evaluation.confidence());
                ticket.setUrgencyUpdatedAt(now);
                updatedCount++;
            } catch (Exception ex) {
                log.warn("Urgency re-score failed for ticketId={}: {}", ticket.getId(), ex.getMessage());
            }
        }

        if (updatedCount > 0) {
            ticketRepository.saveAll(candidates);
        }
        return updatedCount;
    }
}
