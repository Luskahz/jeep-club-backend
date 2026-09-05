package com.jeepclub.backend.iam.authentication.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "authentication_password_change_challenges",
        indexes = {
                @Index(
                        name = "idx_password_change_challenges_user_id",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_password_change_challenges_token_hash",
                        columnList = "token_hash"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_password_change_challenges_token_hash",
                        columnNames = "token_hash"
                )
        }
)
public class PasswordChangeChallengeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, length = 128)
    private String tokenHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "used", nullable = false)
    private boolean used;
}