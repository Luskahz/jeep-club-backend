package com.jeepclub.backend.memberships.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "membership_rejection_notification_attempts", uniqueConstraints =
        @UniqueConstraint(name = "uk_membership_rejection_automatic", columnNames = {"notification_id", "automatic_number"}), indexes =
        @Index(name = "ix_membership_rejection_history", columnList = "notification_id,id"))
@Getter @Setter
public class RejectionNotificationAttemptEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long notificationId;
    @Column(nullable = false, length = 16) private String origin;
    private Long actorId;
    private Integer automaticNumber;
    @Column(nullable = false, length = 32) private String result;
    @Column(nullable = false) private Instant startedAt;
    private Instant finishedAt;
}
