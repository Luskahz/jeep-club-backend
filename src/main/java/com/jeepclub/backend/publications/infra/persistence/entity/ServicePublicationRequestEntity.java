package com.jeepclub.backend.publications.infra.persistence.entity;

import com.jeepclub.backend.publications.core.domain.enums.ServicePublicationRequestStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_publication_requests", indexes = {
        @Index(name = "idx_service_requests_requester", columnList = "requested_by_user_id"),
        @Index(name = "idx_service_requests_status", columnList = "status")
})
@Getter
@Setter
public class ServicePublicationRequestEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "requested_by_user_id", nullable = false)
    private Long requestedByUserId;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    @Column(name = "contact_phone", nullable = false, length = 30)
    private String contactPhone;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ServicePublicationRequestStatus status;
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
    @Column(name = "reviewed_by_user_id")
    private Long reviewedByUserId;
    @Column(name = "created_publication_id")
    private Long createdPublicationId;
    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;
    @Column(name = "reviewed_at")
    private Instant reviewedAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private Long version;
    @ElementCollection
    @CollectionTable(name = "service_publication_request_images", joinColumns = @JoinColumn(name = "request_id"),
            uniqueConstraints = {
                    @UniqueConstraint(name = "uk_service_request_image_position", columnNames = {"request_id", "position"}),
                    @UniqueConstraint(name = "uk_service_request_image_key", columnNames = {"request_id", "storage_key"})
            })
    @OrderBy("position ASC")
    private List<PublicationImageEntity> images = new ArrayList<>();
}
