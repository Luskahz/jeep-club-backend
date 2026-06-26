package com.jeepclub.backend.authentication.core.domain.model;

import com.jeepclub.backend.authentication.core.domain.exception.passwordchangechallenge.PasswordChangeChallengeStateException;
import com.jeepclub.backend.authentication.core.domain.exception.passwordchangechallenge.PasswordChangeChallengeValidationException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PasswordChangeChallenge {

    private static final int MAX_TOKEN_HASH_LENGTH = 128;

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
        this.createdAt = requireValue(
                createdAt,
                "createdAt"
        );
        this.expiresAt = requireValue(
                expiresAt,
                "expiresAt"
        );
        this.usedAt = usedAt;
        this.used = used;

        validateExpiration(
                this.createdAt,
                this.expiresAt
        );

        validateUsedState(
                this.used,
                this.createdAt,
                this.expiresAt,
                this.usedAt
        );
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
        validateId(id);

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
        Instant validatedNow = requireValue(
                now,
                "now"
        );

        return !used
                && validatedNow.isBefore(expiresAt);
    }

    public boolean isExpired(Instant now) {
        Instant validatedNow = requireValue(
                now,
                "now"
        );

        return !validatedNow.isBefore(expiresAt);
    }

    public void markAsUsed(Instant now) {
        Instant validatedNow = requireValue(
                now,
                "now"
        );

        if (used) {
            throw new PasswordChangeChallengeStateException(
                    "Password change challenge has already been used."
            );
        }

        if (!validatedNow.isBefore(expiresAt)) {
            throw new PasswordChangeChallengeStateException(
                    "Password change challenge has expired."
            );
        }

        this.used = true;
        this.usedAt = validatedNow;
    }

    private static void validateId(Long id) {
        if (id == null) {
            throw new PasswordChangeChallengeValidationException(
                    "id cannot be null."
            );
        }

        if (id <= 0) {
            throw new PasswordChangeChallengeValidationException(
                    "id must be positive."
            );
        }
    }

    private static Long validateUserId(Long userId) {
        if (userId == null) {
            throw new PasswordChangeChallengeValidationException(
                    "userId cannot be null."
            );
        }

        if (userId <= 0) {
            throw new PasswordChangeChallengeValidationException(
                    "userId must be positive."
            );
        }

        return userId;
    }

    private static String validateTokenHash(
            String tokenHash
    ) {
        if (tokenHash == null) {
            throw new PasswordChangeChallengeValidationException(
                    "tokenHash cannot be null."
            );
        }

        String normalizedTokenHash =
                tokenHash.trim();

        if (normalizedTokenHash.isBlank()) {
            throw new PasswordChangeChallengeValidationException(
                    "tokenHash cannot be blank."
            );
        }

        if (
                normalizedTokenHash.length()
                        > MAX_TOKEN_HASH_LENGTH
        ) {
            throw new PasswordChangeChallengeValidationException(
                    "tokenHash cannot exceed "
                            + MAX_TOKEN_HASH_LENGTH
                            + " characters."
            );
        }

        return normalizedTokenHash;
    }

    private static void validateExpiration(
            Instant createdAt,
            Instant expiresAt
    ) {
        if (!expiresAt.isAfter(createdAt)) {
            throw new PasswordChangeChallengeValidationException(
                    "expiresAt must be after createdAt."
            );
        }
    }

    private static void validateUsedState(
            boolean used,
            Instant createdAt,
            Instant expiresAt,
            Instant usedAt
    ) {
        if (!used) {
            if (usedAt != null) {
                throw new PasswordChangeChallengeValidationException(
                        "usedAt must be null when challenge is not used."
                );
            }

            return;
        }

        if (usedAt == null) {
            throw new PasswordChangeChallengeValidationException(
                    "usedAt is required when challenge is used."
            );
        }

        if (usedAt.isBefore(createdAt)) {
            throw new PasswordChangeChallengeValidationException(
                    "usedAt cannot be before createdAt."
            );
        }

        if (!usedAt.isBefore(expiresAt)) {
            throw new PasswordChangeChallengeValidationException(
                    "usedAt must be before expiresAt."
            );
        }
    }

    private static <T> T requireValue(
            T value,
            String fieldName
    ) {
        if (value == null) {
            throw new PasswordChangeChallengeValidationException(
                    fieldName + " cannot be null."
            );
        }

        return value;
    }
}