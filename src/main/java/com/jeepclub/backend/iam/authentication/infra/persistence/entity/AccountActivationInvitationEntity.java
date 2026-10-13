package com.jeepclub.backend.iam.authentication.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "authentication_account_activation_invitations")
@Getter
@Setter
public class AccountActivationInvitationEntity {
    @Id
    @Column(name = "identity_id")
    private Long identityId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "delivery_status", nullable = false, length = 24)
    private String deliveryStatus;

    @Version
    private long version;
}
