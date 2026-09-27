package com.jeepclub.backend.publications.infra.persistence.entity;

import com.jeepclub.backend.publications.core.domain.enums.PublicationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "publication_history", uniqueConstraints = @UniqueConstraint(name = "uk_publication_history_original", columnNames = "publication_id"),
        indexes = @Index(name = "idx_publication_history_deleted_at", columnList = "deleted_at"))
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
public abstract class PublicationHistoryEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "publication_id", nullable = false, updatable = false)
    private Long publicationId;
    @Column(name = "author_user_id", nullable = false)
    private Long authorUserId;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private PublicationStatus status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "published_at")
    private Instant publishedAt;
    @Column(name = "archived_at")
    private Instant archivedAt;
    @Column(name = "deleted_by_user_id", nullable = false)
    private Long deletedByUserId;
    @Column(name = "deleted_at", nullable = false)
    private Instant deletedAt;
    @ElementCollection
    @CollectionTable(name = "publication_image_history", joinColumns = @JoinColumn(name = "history_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_publication_history_image_position", columnNames = {"history_id", "position"}))
    @OrderBy("position ASC")
    private List<PublicationImageEntity> images = new ArrayList<>();
}
