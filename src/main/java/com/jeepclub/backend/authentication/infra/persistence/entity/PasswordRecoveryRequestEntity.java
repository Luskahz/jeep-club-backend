package com.jeepclub.backend.authentication.infra.persistence.entity;

import com.jeepclub.backend.authentication.core.domain.enums.PasswordRecoveryRequestStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "authorization_password_reset_requests")
@Getter
@Setter
public class PasswordRecoveryRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column
    private Instant usedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PasswordRecoveryRequestStatus status;
}
