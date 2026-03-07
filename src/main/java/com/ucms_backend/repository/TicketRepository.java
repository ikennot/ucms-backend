package com.ucms_backend.repository;

import com.ucms_backend.model.entity.Ticket;
import com.ucms_backend.model.enums.TicketStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    List<Ticket> findByUserId(UUID userId);

    long countByTicketNumberStartingWith(String prefix);

    long countByStatusIn(List<TicketStatus> statuses);

    @Query("SELECT t.categoryId, COUNT(t) FROM Ticket t GROUP BY t.categoryId")
    List<Object[]> countGroupedByCategory();

    List<Ticket> findByStatusInOrderByCreatedAtAsc(List<TicketStatus> statuses);
}
