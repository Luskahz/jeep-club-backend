package com.jeepclub.backend.memberships.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "membership_rejection_notifications", uniqueConstraints =
        @UniqueConstraint(name = "uk_membership_rejection_decision", columnNames = {"application_id", "type"}), indexes = {
        @Index(name = "ix_membership_rejection_due", columnList = "state,next_attempt_at,id"),
        @Index(name = "ix_membership_rejection_lease", columnList = "state,lease_until,id")})
@Getter @Setter
public class RejectionNotificationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long applicationId;
    @Column(nullable = false) private Long decisionVersion;
    @Column(nullable = false, length = 32) private String type = "MEMBERSHIP_REJECTED";
    @Column(nullable = false, length = 16) private String state = "PENDING";
    @Column(nullable = false) private int automaticAttempts;
    private Instant nextAttemptAt;
    private Instant leaseUntil;
    private Long currentAttemptId;
    @Column(length = 16) private String lastOrigin;
    @Column(length = 32) private String lastResult;
    private Instant lastAttemptAt;
    private Instant smtpAcceptedAt;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    @Version @Column(nullable = false) private Long version;
}
