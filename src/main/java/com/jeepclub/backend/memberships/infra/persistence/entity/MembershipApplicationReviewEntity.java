package com.jeepclub.backend.memberships.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "membership_application_review_history", indexes = @Index(name = "idx_membership_review_application", columnList = "application_id,id"))
@Getter @Setter
public class MembershipApplicationReviewEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "application_id", nullable = false, updatable = false) private Long applicationId;
    @Column(nullable = false, length = 40, updatable = false) private String operation;
    @Column(name = "previous_status", nullable = false, length = 20, updatable = false) private String previousStatus;
    @Column(name = "resulting_status", nullable = false, length = 20, updatable = false) private String resultingStatus;
    @Column(name = "previous_reviewer_id", updatable = false) private Long previousReviewerId;
    @Column(name = "previous_reviewed_at", updatable = false) private Instant previousReviewedAt;
    @Column(name = "previous_finished_at", updatable = false) private Instant previousFinishedAt;
    @Column(name = "original_user_id", updatable = false) private Long originalUserId;
    @Column(name = "actor_id", nullable = false, updatable = false) private Long actorId;
    @Column(name = "occurred_at", nullable = false, updatable = false) private Instant occurredAt;
}
