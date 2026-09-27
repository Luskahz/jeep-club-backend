package com.jeepclub.backend.publications.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "publication_comments", indexes = @Index(name = "idx_publication_comments_publication_created", columnList = "publication_id,created_at"))
@Getter
@Setter
public class PublicationCommentEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publication_id", nullable = false)
    private PublicationEntity publication;
    @Column(name = "author_user_id", nullable = false)
    private Long authorUserId;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @ElementCollection
    @CollectionTable(name = "publication_comment_images", joinColumns = @JoinColumn(name = "comment_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_publication_comment_image_position", columnNames = {"comment_id", "position"}))
    @OrderBy("position ASC")
    private List<PublicationCommentImageEntity> images = new ArrayList<>();
}
