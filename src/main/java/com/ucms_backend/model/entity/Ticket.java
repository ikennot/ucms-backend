package com.ucms_backend.model.entity;

import com.ucms_backend.model.enums.TicketStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ticket")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "assigned_admin_id")
    private UUID assignedAdminId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "ticket_number", nullable = false, unique = true)
    private String ticketNumber;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @Column(name = "confirmed_resolved", nullable = false)
    private boolean confirmedResolved;

    @Column(name = "is_archived", nullable = false)
    private boolean archived;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "urgency_score")
    private Integer urgencyScore;

    @Column(name = "urgency_label", length = 20)
    private String urgencyLabel;

    @Column(name = "urgency_reason", length = 500)
    private String urgencyReason;

    @Column(name = "urgency_signals", length = 1000)
    private String urgencySignals;

    @Column(name = "urgency_confidence")
    private Double urgencyConfidence;

    @Column(name = "urgency_updated_at")
    private LocalDateTime urgencyUpdatedAt;

    @Column(name = "is_overridden", nullable = false)
    private boolean urgencyOverridden;

    @Column(name = "urgency_override_reason", length = 500)
    private String urgencyOverrideReason;

    @PrePersist
    void onPrePersist() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = TicketStatus.PENDING;
        }
        archived = archived || status == TicketStatus.CLOSED;
    }

    @PreUpdate
    void onPreUpdate() {
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}
