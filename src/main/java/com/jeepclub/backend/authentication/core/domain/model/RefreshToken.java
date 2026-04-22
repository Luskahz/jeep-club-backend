package com.jeepclub.backend.authentication.core.domain.model;

import com.jeepclub.backend.authentication.core.domain.enums.RefreshTokenStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

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
            Duration ttl
    ) {
        Instant now = Instant.now();

        this.session = session;
        this.tokenHash = tokenHash;
        this.createdAt = now;
        this.expiresAt = now.plus(ttl);
        this.status = RefreshTokenStatus.ACTIVE;
    }

    @Contract("_, _, _ -> new")
    public static @NotNull RefreshToken create(
            Session session,
            String tokenHash,
            Duration ttl
    ) {
        validateCreation(session, tokenHash, ttl);

        return new RefreshToken(
                session,
                tokenHash,
                ttl
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
            throw new IllegalArgumentException("expiresAt must be after createdAt");
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
            throw new IllegalStateException("Only active refresh token can be revoked");
        }

        this.status = RefreshTokenStatus.REVOKED;
    }

    public void rotate(
            Long newRefreshTokenId,
            Instant now
    ) {
        Objects.requireNonNull(newRefreshTokenId, "newRefreshTokenId is required");
        Objects.requireNonNull(now, "now is required");

        if (!isValid(now)) {
            throw new IllegalStateException("RefreshToken inválido, não pode ser rotacionado");
        }

        if (this.replacedByTokenId != null) {
            throw new IllegalStateException("RefreshToken já foi rotacionado");
        }

        this.replacedByTokenId = newRefreshTokenId;
        this.status = RefreshTokenStatus.ROTATED;
    }

    private static void validateCreation(
            Session session,
            String tokenHash,
            Duration ttl
    ) {
        Objects.requireNonNull(session, "session is required");
        validateTokenHash(tokenHash);
        Objects.requireNonNull(ttl, "ttl is required");

        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be greater than zero");
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