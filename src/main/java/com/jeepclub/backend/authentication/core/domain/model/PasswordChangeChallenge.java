package com.jeepclub.backend.authentication.core.domain.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PasswordChangeChallenge {

    private Long id;
    private Long userId;
    private String tokenHash;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant usedAt;
    private boolean used;

    private PasswordChangeChallenge(
            Long id,
            Long userId,
            String tokenHash,
            Instant createdAt,
            Instant expiresAt,
            Instant usedAt,
            boolean used
    ) {
        this.id = id;
        this.userId = validateUserId(userId);
        this.tokenHash = validateTokenHash(tokenHash);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt cannot be null");
        this.usedAt = usedAt;
        this.used = used;

        validateExpiration(createdAt, expiresAt);
        validateUsedState(used, usedAt);
    }

    public static PasswordChangeChallenge create(
            Long userId,
            String tokenHash,
            Instant createdAt,
            Instant expiresAt
    ) {
        return new PasswordChangeChallenge(
                null,
                userId,
                tokenHash,
                createdAt,
                expiresAt,
                null,
                false
        );
    }

    public static PasswordChangeChallenge reconstitute(
            Long id,
            Long userId,
            String tokenHash,
            Instant createdAt,
            Instant expiresAt,
            Instant usedAt,
            boolean used
    ) {
        return new PasswordChangeChallenge(
                id,
                userId,
                tokenHash,
                createdAt,
                expiresAt,
                usedAt,
                used
        );
    }

    public boolean isValid(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        return !used && now.isBefore(expiresAt);
    }

    public void markAsUsed(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        if (!isValid(now)) {
            throw new IllegalStateException("Password change challenge is invalid or expired.");
        }

        this.used = true;
        this.usedAt = now;
    }

    private static Long validateUserId(Long userId) {
        Objects.requireNonNull(userId, "userId cannot be null");

        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive.");
        }

        return userId;
    }

    private static String validateTokenHash(String tokenHash) {
        if (tokenHash == null || tokenHash.isBlank()) {
            throw new IllegalArgumentException("tokenHash cannot be null or blank.");
        }

        return tokenHash;
    }

    private static void validateExpiration(
            Instant createdAt,
            Instant expiresAt
    ) {
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("expiresAt must be after createdAt.");
        }
    }

    private static void validateUsedState(
            boolean used,
            Instant usedAt
    ) {
        if (used && usedAt == null) {
            throw new IllegalArgumentException("usedAt is required when challenge is used.");
        }

        if (!used && usedAt != null) {
            throw new IllegalArgumentException("usedAt must be null when challenge is not used.");
        }
    }
}