package com.ucms_backend.repository;

import com.ucms_backend.model.entity.Ticket;
import com.ucms_backend.model.enums.TicketStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    List<Ticket> findByUserId(UUID userId);

    List<Ticket> findByUserIdAndArchivedFalse(UUID userId);

    long countByTicketNumberStartingWith(String prefix);

    long countByStatusIn(List<TicketStatus> statuses);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime startInclusive, LocalDateTime endExclusive);

    long countByStatusInAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThan(
        List<TicketStatus> statuses,
        LocalDateTime startInclusive,
        LocalDateTime endExclusive
    );

    List<Ticket> findByStatusInAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThan(
        List<TicketStatus> statuses,
        LocalDateTime startInclusive,
        LocalDateTime endExclusive
    );

    @Query("SELECT t.categoryId, COUNT(t) FROM Ticket t GROUP BY t.categoryId")
    List<Object[]> countGroupedByCategory();

    @Query("""
            SELECT t
            FROM Ticket t
            WHERE t.status IN :statuses
              AND t.urgencyOverridden = FALSE
              AND (t.urgencyUpdatedAt IS NULL OR t.urgencyUpdatedAt <= :cutoff)
            ORDER BY t.createdAt ASC
            """)
    List<Ticket> findForUrgencyRefresh(
            @Param("statuses") List<TicketStatus> statuses,
            @Param("cutoff") LocalDateTime cutoff,
            Pageable pageable
    );

    List<Ticket> findByStatusInOrderByCreatedAtAsc(List<TicketStatus> statuses);

    List<Ticket> findByStatusInOrderByCreatedAtAsc(List<TicketStatus> statuses, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);
}
