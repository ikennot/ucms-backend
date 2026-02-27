package com.ucms_backend.repository;

import com.ucms_backend.model.entity.Ticket;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    List<Ticket> findByUserId(UUID userId);

    long countByTicketNumberStartingWith(String prefix);
}
