package com.jeepclub.backend.publications.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "publication_likes", uniqueConstraints = @UniqueConstraint(name = "uk_publication_like_member", columnNames = {"publication_id", "member_user_id"}),
        indexes = @Index(name = "idx_publication_likes_member", columnList = "member_user_id"))
@Getter
@Setter
public class PublicationLikeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publication_id", nullable = false)
    private PublicationEntity publication;
    @Column(name = "member_user_id", nullable = false)
    private Long memberUserId;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
