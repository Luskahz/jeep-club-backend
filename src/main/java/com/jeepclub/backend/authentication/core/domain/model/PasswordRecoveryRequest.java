package com.jeepclub.backend.authentication.core.domain.model;

import com.jeepclub.backend.authentication.core.domain.enums.PasswordRecoveryRequestMethod;
import com.jeepclub.backend.authentication.core.domain.enums.PasswordRecoveryRequestStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PasswordRecoveryRequest {

    private Long id;
    private Long userId;
    private String tokenHash;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant resolvedAt;
    private Instant cancelledAt;
    private PasswordRecoveryRequestStatus status;
    private PasswordRecoveryRequestMethod method;

    private PasswordRecoveryRequest(
            Long id,
            Long userId,
            String tokenHash,
            Instant createdAt,
            Instant expiresAt,
            Instant resolvedAt,
            Instant cancelledAt,
            PasswordRecoveryRequestStatus status,
            PasswordRecoveryRequestMethod method
    ) {
        this.id = id;
        this.userId = validateUserId(userId);
        this.tokenHash = normalizeTokenHash(tokenHash);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt cannot be null");
        this.resolvedAt = resolvedAt;
        this.cancelledAt = cancelledAt;
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.method = Objects.requireNonNull(method, "method cannot be null");

        validateExpiration(this.createdAt, this.expiresAt);
        validateTokenRequirement(this.method, this.tokenHash);
        validateResolvedState(this.status, this.resolvedAt);
        validateCancelledState(this.status, this.cancelledAt);
    }

    public static PasswordRecoveryRequest createOpenRequest(
            Long userId,
            Instant createdAt,
            Instant expiresAt
    ) {
        return new PasswordRecoveryRequest(
                null,
                userId,
                null,
                createdAt,
                expiresAt,
                null,
                null,
                PasswordRecoveryRequestStatus.OPEN,
                PasswordRecoveryRequestMethod.UNDEFINED
        );
    }

    public static PasswordRecoveryRequest reconstitute(
            Long id,
            Long userId,
            String tokenHash,
            Instant expiresAt,
            Instant createdAt,
            Instant resolvedAt,
            Instant cancelledAt,
            PasswordRecoveryRequestStatus status,
            PasswordRecoveryRequestMethod method
    ) {
        return new PasswordRecoveryRequest(
                id,
                userId,
                tokenHash,
                createdAt,
                expiresAt,
                resolvedAt,
                cancelledAt,
                status,
                method
        );
    }

    public void changeToEmailTokenMethod(
            String tokenHash,
            Instant now
    ) {
        Objects.requireNonNull(now, "now cannot be null");

        assertOpen(now);

        String normalizedTokenHash = validateRequiredTokenHash(tokenHash);

        this.method = PasswordRecoveryRequestMethod.EMAIL_TOKEN;
        this.tokenHash = normalizedTokenHash;
    }

    public void changeToAdminResetLinkMethod(
            String tokenHash,
            Instant now
    ) {
        Objects.requireNonNull(now, "now cannot be null");

        assertOpen(now);

        String normalizedTokenHash = validateRequiredTokenHash(tokenHash);

        this.method = PasswordRecoveryRequestMethod.ADMIN_RESET_LINK;
        this.tokenHash = normalizedTokenHash;
    }

    public void changeToAdminTemporaryPasswordMethod(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        assertOpen(now);

        this.method = PasswordRecoveryRequestMethod.ADMIN_TEMPORARY_PASSWORD;
        this.tokenHash = null;
    }

    public void resolve(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        assertOpen(now);

        this.status = PasswordRecoveryRequestStatus.RESOLVED;
        this.resolvedAt = now;
    }

    public void cancel(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        if (!canBeCancelled()) {
            return;
        }

        this.status = PasswordRecoveryRequestStatus.CANCELLED;
        this.cancelledAt = now;
    }

    public boolean isOpen(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        return status == PasswordRecoveryRequestStatus.OPEN
                && !isExpired(now);
    }

    public boolean isExpired(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        return !now.isBefore(expiresAt);
    }

    public boolean canBeCancelled() {
        return status == PasswordRecoveryRequestStatus.OPEN;
    }

    public boolean isTokenBased() {
        return method == PasswordRecoveryRequestMethod.EMAIL_TOKEN
                || method == PasswordRecoveryRequestMethod.ADMIN_RESET_LINK;
    }

    public boolean isTemporaryPasswordBased() {
        return method == PasswordRecoveryRequestMethod.ADMIN_TEMPORARY_PASSWORD;
    }

    public boolean hasUndefinedMethod() {
        return method == PasswordRecoveryRequestMethod.UNDEFINED;
    }

    private void assertOpen(Instant now) {
        if (!isOpen(now)) {
            throw new IllegalStateException("Password recovery request is not open or has expired.");
        }
    }

    private static Long validateUserId(Long userId) {
        Objects.requireNonNull(userId, "userId cannot be null");

        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive.");
        }

        return userId;
    }

    private static String normalizeTokenHash(String tokenHash) {
        if (tokenHash == null) {
            return null;
        }

        String normalized = tokenHash.trim();

        if (normalized.isBlank()) {
            return null;
        }

        return normalized;
    }

    private static String validateRequiredTokenHash(String tokenHash) {
        String normalizedTokenHash = normalizeTokenHash(tokenHash);

        if (normalizedTokenHash == null) {
            throw new IllegalArgumentException("tokenHash cannot be null or blank.");
        }

        return normalizedTokenHash;
    }

    private static void validateExpiration(
            Instant createdAt,
            Instant expiresAt
    ) {
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("expiresAt must be after createdAt.");
        }
    }

    private static void validateTokenRequirement(
            PasswordRecoveryRequestMethod method,
            String tokenHash
    ) {
        boolean tokenRequired = method == PasswordRecoveryRequestMethod.EMAIL_TOKEN
                || method == PasswordRecoveryRequestMethod.ADMIN_RESET_LINK;

        if (tokenRequired && tokenHash == null) {
            throw new IllegalArgumentException("tokenHash is required for token-based password recovery requests.");
        }

        boolean tokenForbidden = method == PasswordRecoveryRequestMethod.UNDEFINED
                || method == PasswordRecoveryRequestMethod.ADMIN_TEMPORARY_PASSWORD;

        if (tokenForbidden && tokenHash != null) {
            throw new IllegalArgumentException("tokenHash must be null for non-token password recovery requests.");
        }
    }

    private static void validateResolvedState(
            PasswordRecoveryRequestStatus status,
            Instant resolvedAt
    ) {
        if (status == PasswordRecoveryRequestStatus.RESOLVED && resolvedAt == null) {
            throw new IllegalArgumentException("resolvedAt is required when status is RESOLVED.");
        }

        if (status != PasswordRecoveryRequestStatus.RESOLVED && resolvedAt != null) {
            throw new IllegalArgumentException("resolvedAt must be null when status is not RESOLVED.");
        }
    }

    private static void validateCancelledState(
            PasswordRecoveryRequestStatus status,
            Instant cancelledAt
    ) {
        if (status == PasswordRecoveryRequestStatus.CANCELLED && cancelledAt == null) {
            throw new IllegalArgumentException("cancelledAt is required when status is CANCELLED.");
        }

        if (status != PasswordRecoveryRequestStatus.CANCELLED && cancelledAt != null) {
            throw new IllegalArgumentException("cancelledAt must be null when status is not CANCELLED.");
        }
    }
}