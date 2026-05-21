package com.jeepclub.backend.authentication.infra.persistence.entity;

import com.jeepclub.backend.authentication.core.domain.enums.PasswordRecoveryRequestMethod;
import com.jeepclub.backend.authentication.core.domain.enums.PasswordRecoveryRequestStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "authentication_password_recovery_requests",
        indexes = {
                @Index(
                        name = "idx_password_recovery_requests_user_id",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_password_recovery_requests_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_password_recovery_requests_method",
                        columnList = "method"
                ),
                @Index(
                        name = "idx_password_recovery_requests_expires_at",
                        columnList = "expires_at"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_password_recovery_requests_token_hash",
                        columnNames = "token_hash"
                )
        }
)
@Getter
@Setter
public class PasswordRecoveryRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "user_id",
            nullable = false
    )
    private Long userId;

    @Column(
            name = "token_hash",
            length = 128
    )
    private String tokenHash;

    @Column(
            name = "created_at",
            nullable = false
    )
    private Instant createdAt;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private Instant expiresAt;

    @Column(
            name = "resolved_at"
    )
    private Instant resolvedAt;

    @Column(
            name = "cancelled_at"
    )
    private Instant cancelledAt;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private PasswordRecoveryRequestStatus status;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "method",
            nullable = false,
            length = 50
    )
    private PasswordRecoveryRequestMethod method;
}