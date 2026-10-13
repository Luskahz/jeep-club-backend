package com.jeepclub.backend.iam.authentication.core.domain.model;

import java.time.Instant;
import java.util.Objects;

public class AccountActivationInvitation {
    private final Long identityId;
    private String tokenHash;
    private Instant expiresAt;
    private Instant usedAt;
    private String deliveryStatus;
    private long version;

    private AccountActivationInvitation(Long identityId, String tokenHash, Instant expiresAt,
                                        Instant usedAt, String deliveryStatus, long version) {
        this.identityId = Objects.requireNonNull(identityId);
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.usedAt = usedAt;
        this.deliveryStatus = Objects.requireNonNull(deliveryStatus);
        this.version = version;
    }

    public static AccountActivationInvitation create(Long identityId, String tokenHash, Instant expiresAt) {
        return new AccountActivationInvitation(identityId, tokenHash, expiresAt, null, "QUEUED", 0);
    }

    public static AccountActivationInvitation reconstitute(Long identityId, String tokenHash, Instant expiresAt,
                                                            Instant usedAt, String deliveryStatus, long version) {
        return new AccountActivationInvitation(identityId, tokenHash, expiresAt, usedAt, deliveryStatus, version);
    }

    public void rotate(String newTokenHash, Instant newExpiresAt) {
        tokenHash = Objects.requireNonNull(newTokenHash);
        expiresAt = Objects.requireNonNull(newExpiresAt);
        usedAt = null;
        deliveryStatus = "QUEUED";
    }

    public void markUsed(Instant now) {
        usedAt = Objects.requireNonNull(now);
    }

    public Long identityId() { return identityId; }
    public String tokenHash() { return tokenHash; }
    public Instant expiresAt() { return expiresAt; }
    public Instant usedAt() { return usedAt; }
    public String deliveryStatus() { return deliveryStatus; }
    public long version() { return version; }
}
