package com.jeepclub.backend.iam.authentication.core.domain.model;

import com.jeepclub.backend.iam.authentication.core.domain.enums.RefreshTokenStatus;
import com.jeepclub.backend.iam.authentication.core.domain.exception.refreshtoken.RefreshTokenStateException;
import com.jeepclub.backend.iam.authentication.core.domain.exception.refreshtoken.RefreshTokenValidationException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RefreshToken {

    private static final int MAX_TOKEN_HASH_LENGTH = 255;

    private Long id;
    private Session session;
    private String tokenHash;
    private Instant createdAt;
    private Instant expiresAt;
    private RefreshTokenStatus status;
    private Long replacedByTokenId;

    private RefreshToken(
            Session session,
            String tokenHash,
            Duration ttl,
            Instant now
    ) {
        this.session = session;
        this.tokenHash = tokenHash;
        this.createdAt = now;
        this.expiresAt = now.plus(ttl);
        this.status = RefreshTokenStatus.ACTIVE;
        this.replacedByTokenId = null;
    }

    public static RefreshToken create(
            Session session,
            String tokenHash,
            Duration ttl,
            Instant now
    ) {
        Session validatedSession =
                validateSession(session);

        String normalizedTokenHash =
                validateTokenHash(tokenHash);

        Duration validatedTtl =
                validateTtl(ttl);

        Instant validatedNow =
                requireValue(now, "now");

        return new RefreshToken(
                validatedSession,
                normalizedTokenHash,
                validatedTtl,
                validatedNow
        );
    }

    public static RefreshToken reconstitute(
            Long id,
            Session session,
            String tokenHash,
            Instant createdAt,
            Instant expiresAt,
            RefreshTokenStatus status,
            Long replacedByTokenId
    ) {
        validateId(id);

        Session validatedSession =
                validateSession(session);

        String normalizedTokenHash =
                validateTokenHash(tokenHash);

        Instant validatedCreatedAt =
                requireValue(
                        createdAt,
                        "createdAt"
                );

        Instant validatedExpiresAt =
                requireValue(
                        expiresAt,
                        "expiresAt"
                );

        RefreshTokenStatus validatedStatus =
                requireValue(
                        status,
                        "status"
                );

        validateExpiration(
                validatedCreatedAt,
                validatedExpiresAt
        );

        validateReplacementTokenId(
                replacedByTokenId
        );

        validateStatusConsistency(
                validatedStatus,
                replacedByTokenId
        );

        RefreshToken token =
                new RefreshToken();

        token.id = id;
        token.session = validatedSession;
        token.tokenHash = normalizedTokenHash;
        token.createdAt = validatedCreatedAt;
        token.expiresAt = validatedExpiresAt;
        token.status = validatedStatus;
        token.replacedByTokenId = replacedByTokenId;

        return token;
    }

    public boolean isExpired(Instant now) {
        Instant validatedNow =
                requireValue(now, "now");

        return !validatedNow.isBefore(expiresAt);
    }

    public boolean isActive(Instant now) {
        Instant validatedNow =
                requireValue(now, "now");

        return status == RefreshTokenStatus.ACTIVE
                && !isExpired(validatedNow);
    }

    public boolean isValid(Instant now) {
        Instant validatedNow =
                requireValue(now, "now");

        return isActive(validatedNow)
                && session.isValid(validatedNow);
    }

    public boolean isRevoked() {
        return status == RefreshTokenStatus.REVOKED;
    }

    public boolean isRotated() {
        return status == RefreshTokenStatus.ROTATED;
    }

    public void revoke(Instant now) {
        Instant validatedNow =
                requireValue(now, "now");

        if (status != RefreshTokenStatus.ACTIVE) {
            throw new RefreshTokenStateException(
                    "Only active refresh tokens can be revoked."
            );
        }

        if (isExpired(validatedNow)) {
            throw new RefreshTokenStateException(
                    "Expired refresh tokens cannot be revoked."
            );
        }

        this.status = RefreshTokenStatus.REVOKED;
        this.replacedByTokenId = null;
    }

    public void rotate(
            Long newRefreshTokenId,
            Instant now
    ) {
        Long validatedNewRefreshTokenId =
                validateRequiredReplacementTokenId(
                        newRefreshTokenId
                );

        Instant validatedNow =
                requireValue(now, "now");

        if (!isValid(validatedNow)) {
            throw new RefreshTokenStateException(
                    "Only valid refresh tokens can be rotated."
            );
        }

        if (replacedByTokenId != null) {
            throw new RefreshTokenStateException(
                    "Refresh token has already been rotated."
            );
        }

        this.replacedByTokenId =
                validatedNewRefreshTokenId;

        this.status =
                RefreshTokenStatus.ROTATED;
    }

    private static void validateId(Long id) {
        if (id == null) {
            throw new RefreshTokenValidationException(
                    "id cannot be null."
            );
        }

        if (id <= 0) {
            throw new RefreshTokenValidationException(
                    "id must be positive."
            );
        }
    }

    private static Session validateSession(
            Session session
    ) {
        return requireValue(
                session,
                "session"
        );
    }

    private static String validateTokenHash(
            String tokenHash
    ) {
        if (tokenHash == null) {
            throw new RefreshTokenValidationException(
                    "tokenHash cannot be null."
            );
        }

        String normalizedTokenHash =
                tokenHash.trim();

        if (normalizedTokenHash.isBlank()) {
            throw new RefreshTokenValidationException(
                    "tokenHash cannot be blank."
            );
        }

        if (
                normalizedTokenHash.length()
                        > MAX_TOKEN_HASH_LENGTH
        ) {
            throw new RefreshTokenValidationException(
                    "tokenHash cannot exceed "
                            + MAX_TOKEN_HASH_LENGTH
                            + " characters."
            );
        }

        return normalizedTokenHash;
    }

    private static Duration validateTtl(
            Duration ttl
    ) {
        Duration validatedTtl =
                requireValue(ttl, "ttl");

        if (
                validatedTtl.isZero()
                        || validatedTtl.isNegative()
        ) {
            throw new RefreshTokenValidationException(
                    "ttl must be greater than zero."
            );
        }

        return validatedTtl;
    }

    private static void validateExpiration(
            Instant createdAt,
            Instant expiresAt
    ) {
        if (!expiresAt.isAfter(createdAt)) {
            throw new RefreshTokenValidationException(
                    "expiresAt must be after createdAt."
            );
        }
    }

    private static void validateReplacementTokenId(
            Long replacedByTokenId
    ) {
        if (
                replacedByTokenId != null
                        && replacedByTokenId <= 0
        ) {
            throw new RefreshTokenValidationException(
                    "replacedByTokenId must be positive."
            );
        }
    }

    private static Long
    validateRequiredReplacementTokenId(
            Long replacedByTokenId
    ) {
        if (replacedByTokenId == null) {
            throw new RefreshTokenValidationException(
                    "newRefreshTokenId cannot be null."
            );
        }

        if (replacedByTokenId <= 0) {
            throw new RefreshTokenValidationException(
                    "newRefreshTokenId must be positive."
            );
        }

        return replacedByTokenId;
    }

    private static void validateStatusConsistency(
            RefreshTokenStatus status,
            Long replacedByTokenId
    ) {
        if (
                status == RefreshTokenStatus.ACTIVE
                        && replacedByTokenId != null
        ) {
            throw new RefreshTokenValidationException(
                    "ACTIVE refresh token cannot have replacedByTokenId."
            );
        }

        if (
                status == RefreshTokenStatus.ROTATED
                        && replacedByTokenId == null
        ) {
            throw new RefreshTokenValidationException(
                    "ROTATED refresh token must have replacedByTokenId."
            );
        }

        if (
                status == RefreshTokenStatus.REVOKED
                        && replacedByTokenId != null
        ) {
            throw new RefreshTokenValidationException(
                    "REVOKED refresh token cannot have replacedByTokenId."
            );
        }
    }

    private static <T> T requireValue(
            T value,
            String fieldName
    ) {
        if (value == null) {
            throw new RefreshTokenValidationException(
                    fieldName + " cannot be null."
            );
        }

        return value;
    }
}