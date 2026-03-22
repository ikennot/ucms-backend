package com.ucms_backend.repository;

import com.ucms_backend.model.entity.TicketResponse;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TicketResponseRepository extends JpaRepository<TicketResponse, Long> {
    List<TicketResponse> findByTicketIdOrderByCreatedAtAscIdAsc(Long ticketId);

    boolean existsByTicketId(Long ticketId);

    @Query("select distinct tr.ticketId from TicketResponse tr where tr.ticketId in :ticketIds")
    List<Long> findDistinctTicketIdsByTicketIdIn(List<Long> ticketIds);
}
