package com.jeepclub.backend.publications.infra.persistence.entity;

import com.jeepclub.backend.publications.core.domain.enums.ServicePublicationChangeRequestStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_publication_change_requests",
        uniqueConstraints = @UniqueConstraint(name = "uk_service_pending_change", columnNames = "pending_service_publication_id"),
        indexes = {
                @Index(name = "idx_service_changes_service", columnList = "service_publication_id"),
                @Index(name = "idx_service_changes_requester", columnList = "requested_by_user_id"),
                @Index(name = "idx_service_changes_status", columnList = "status"),
                @Index(name = "idx_service_changes_requested_at", columnList = "requested_at")
        })
@Getter @Setter
public class ServicePublicationChangeRequestEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "service_publication_id", nullable = false, updatable = false)
    private Long servicePublicationId;
    // Nullable for terminal requests; UNIQUE permits only one non-null pending key per service.
    @Column(name = "pending_service_publication_id")
    private Long pendingServicePublicationId;
    @Column(name = "requested_by_user_id", nullable = false, updatable = false)
    private Long requestedByUserId;
    @Column(name = "proposed_title", nullable = false, length = 200)
    private String proposedTitle;
    @Column(name = "proposed_content", nullable = false, columnDefinition = "TEXT")
    private String proposedContent;
    @Column(name = "proposed_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal proposedAmount;
    @Column(name = "proposed_contact_phone", nullable = false, length = 30)
    private String proposedContactPhone;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ServicePublicationChangeRequestStatus status;
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
    @Column(name = "reviewed_by_user_id")
    private Long reviewedByUserId;
    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;
    @Column(name = "reviewed_at")
    private Instant reviewedAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private Long version;
    @ElementCollection
    @BatchSize(size = 20)
    @CollectionTable(name = "service_publication_change_request_images",
            joinColumns = @JoinColumn(name = "change_request_id"), uniqueConstraints = {
                    @UniqueConstraint(name = "uk_service_change_image_position", columnNames = {"change_request_id", "position"}),
                    @UniqueConstraint(name = "uk_service_change_image_key", columnNames = {"change_request_id", "storage_key"})
            })
    @OrderBy("position ASC")
    private List<PublicationImageEntity> proposedImages = new ArrayList<>();
}
