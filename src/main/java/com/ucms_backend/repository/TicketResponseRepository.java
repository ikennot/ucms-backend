package com.ucms_backend.repository;

import com.ucms_backend.model.entity.TicketResponse;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketResponseRepository extends JpaRepository<TicketResponse, Long> {
    List<TicketResponse> findByTicketIdOrderByCreatedAtAscIdAsc(Long ticketId);
}
