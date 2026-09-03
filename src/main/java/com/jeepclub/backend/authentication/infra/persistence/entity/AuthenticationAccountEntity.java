package com.jeepclub.backend.authentication.infra.persistence.entity;

import com.jeepclub.backend.authentication.core.domain.enums.AuthenticationAccessStatus;
import com.jeepclub.backend.authentication.core.domain.enums.AuthenticationStatus;
import com.jeepclub.backend.authentication.core.domain.enums.CredentialStatus;
import com.jeepclub.backend.identity.infra.persistence.entity.IdentityEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "authentication_accounts")
public class AuthenticationAccountEntity {

    @Id
    @Column(name = "identity_id", nullable = false, updatable = false)
    private Long identityId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "identity_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_authentication_accounts_identity")
    )
    private IdentityEntity identity;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_status", nullable = false, length = 20)
    private AuthenticationAccessStatus accessStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "authentication_status", nullable = false, length = 20)
    private AuthenticationStatus authenticationStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "credential_status", nullable = false, length = 30)
    private CredentialStatus credentialStatus;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "access_disabled_at")
    private Instant accessDisabledAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;
}
