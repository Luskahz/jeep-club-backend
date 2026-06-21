package com.jeepclub.backend.authentication.core.domain.model;

import com.jeepclub.backend.authentication.core.domain.enums.RefreshTokenStatus;
import com.jeepclub.backend.authentication.core.domain.exception.refreshtoken.RefreshTokenInvalidExpiresAtValueException;
import com.jeepclub.backend.authentication.core.domain.exception.refreshtoken.RefreshTokenStateException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Contract;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RefreshToken {

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
    }

    @Contract("_, _, _, _ -> new")
    public static @NotNull RefreshToken create(
            Session session,
            String tokenHash,
            Duration ttl,
            Instant now
    ) {
        validateCreation(session, tokenHash, ttl, now);

        return new RefreshToken(
                session,
                tokenHash,
                ttl,
                now
        );
    }

    public static @NotNull RefreshToken reconstitute(
            Long id,
            Session session,
            String tokenHash,
            Instant createdAt,
            Instant expiresAt,
            RefreshTokenStatus status,
            Long replacedByTokenId
    ) {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(session, "session is required");
        validateTokenHash(tokenHash);
        Objects.requireNonNull(createdAt, "createdAt is required");
        Objects.requireNonNull(expiresAt, "expiresAt is required");
        Objects.requireNonNull(status, "status is required");

        if (!expiresAt.isAfter(createdAt)) {
            throw new RefreshTokenInvalidExpiresAtValueException("expiresAt must be after createdAt");
        }

        validateStatusConsistency(status, replacedByTokenId);

        RefreshToken token = new RefreshToken();
        token.id = id;
        token.session = session;
        token.tokenHash = tokenHash;
        token.createdAt = createdAt;
        token.expiresAt = expiresAt;
        token.status = status;
        token.replacedByTokenId = replacedByTokenId;
        return token;
    }

    public boolean isExpired(Instant now) {
        Objects.requireNonNull(now, "now is required");
        return !expiresAt.isAfter(now);
    }

    public boolean isActive(Instant now) {
        Objects.requireNonNull(now, "now is required");
        return status == RefreshTokenStatus.ACTIVE && !isExpired(now);
    }

    public boolean isValid(Instant now) {
        Objects.requireNonNull(now, "now is required");
        return isActive(now) && session.isValid(now);
    }

    public boolean isRevoked() {
        return status == RefreshTokenStatus.REVOKED;
    }

    public boolean isRotated() {
        return status == RefreshTokenStatus.ROTATED;
    }

    public void revoke(Instant now) {
        Objects.requireNonNull(now, "now is required");

        if (!isActive(now)) {
            throw new RefreshTokenStateException(
                    "Only active refresh tokens can be revoked."
            );
        }

        status = RefreshTokenStatus.REVOKED;
    }

    public void rotate(
            Long newRefreshTokenId,
            Instant now
    ) {
        Objects.requireNonNull(
                newRefreshTokenId,
                "newRefreshTokenId is required"
        );
        Objects.requireNonNull(now, "now is required");

        if (!isValid(now)) {
            throw new RefreshTokenStateException(
                    "Only valid refresh tokens can be rotated."
            );
        }

        if (replacedByTokenId != null) {
            throw new RefreshTokenStateException(
                    "Refresh token has already been rotated."
            );
        }

        replacedByTokenId = newRefreshTokenId;
        status = RefreshTokenStatus.ROTATED;
    }

    private static void validateCreation(
            Session session,
            String tokenHash,
            Duration ttl,
            Instant now
    ) {
        Objects.requireNonNull(session, "session is required");
        validateTokenHash(tokenHash);
        Objects.requireNonNull(ttl, "ttl is required");
        Objects.requireNonNull(now, "now is required");

        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException(
                    "ttl must be greater than zero"
            );
        }
    }
    private static void validateTokenHash(String tokenHash) {
        if (tokenHash == null || tokenHash.isBlank()) {
            throw new IllegalArgumentException("tokenHash is required");
        }
    }
    private static void validateStatusConsistency(
            RefreshTokenStatus status,
            Long replacedByTokenId
    ) {
        if (status == RefreshTokenStatus.ACTIVE && replacedByTokenId != null) {
            throw new IllegalStateException("ACTIVE token cannot have replacedByTokenId");
        }

        if (status == RefreshTokenStatus.ROTATED && replacedByTokenId == null) {
            throw new IllegalStateException("ROTATED token must have replacedByTokenId");
        }

        if (status == RefreshTokenStatus.REVOKED && replacedByTokenId != null) {
            throw new IllegalStateException("REVOKED token cannot have replacedByTokenId");
        }
    }
}