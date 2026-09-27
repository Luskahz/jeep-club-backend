package com.jeepclub.backend.publications.infra.persistence.entity;

import com.jeepclub.backend.publications.core.domain.enums.PublicationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "publications", indexes = {
        @Index(name = "idx_publications_author", columnList = "author_user_id"),
        @Index(name = "idx_publications_status_published", columnList = "status,published_at")
})
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
public abstract class PublicationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "author_user_id", nullable = false)
    private Long authorUserId;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private PublicationStatus status;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "published_at")
    private Instant publishedAt;
    @Column(name = "archived_at")
    private Instant archivedAt;
    @ElementCollection
    @CollectionTable(name = "publication_images", joinColumns = @JoinColumn(name = "publication_id"),
            uniqueConstraints = {
                    @UniqueConstraint(name = "uk_publication_image_position", columnNames = {"publication_id", "position"}),
                    @UniqueConstraint(name = "uk_publication_image_key", columnNames = {"publication_id", "storage_key"})
            })
    @OrderBy("position ASC")
    private List<PublicationImageEntity> images = new ArrayList<>();
}
