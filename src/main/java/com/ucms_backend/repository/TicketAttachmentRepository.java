package com.ucms_backend.repository;

import com.ucms_backend.model.entity.TicketAttachment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {
    List<TicketAttachment> findByTicketId(Long ticketId);

    long countByTicketId(Long ticketId);
}
