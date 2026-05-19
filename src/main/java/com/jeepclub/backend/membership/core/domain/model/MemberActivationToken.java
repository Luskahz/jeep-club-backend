package com.jeepclub.backend.membership.core.domain.model;

import java.time.Duration;
import java.time.Instant;

public class MemberActivationToken {

    private Long id;
    private Long applicationId;
    private String tokenHash;
    private Instant expiresAt;
    private Instant usedAt;
    private Instant createdAt;

    private MemberActivationToken() {}

    public static MemberActivationToken create(
            Long applicationId,
            String tokenHash,
            Duration ttl,
            Instant now
    ) {
        MemberActivationToken token = new MemberActivationToken();
        token.applicationId = applicationId;
        token.tokenHash = tokenHash;
        token.expiresAt = now.plus(ttl);
        token.usedAt = null;
        token.createdAt = now;
        return token;
    }

    public static MemberActivationToken reconstitute(
            Long id,
            Long applicationId,
            String tokenHash,
            Instant expiresAt,
            Instant usedAt,
            Instant createdAt
    ) {
        MemberActivationToken token = new MemberActivationToken();
        token.id = id;
        token.applicationId = applicationId;
        token.tokenHash = tokenHash;
        token.expiresAt = expiresAt;
        token.usedAt = usedAt;
        token.createdAt = createdAt;
        return token;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isValid(Instant now) {
        return !isExpired(now) && !isUsed();
    }

    public void markAsUsed(Instant now) {
        if (isUsed()) {
            throw new IllegalStateException("Token já foi utilizado.");
        }
        this.usedAt = now;
    }

    public Long getId() { return id; }
    public Long getApplicationId() { return applicationId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getUsedAt() { return usedAt; }
    public Instant getCreatedAt() { return createdAt; }
}