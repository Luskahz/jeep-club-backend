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
        this.tokenHash = tokenHash;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt cannot be null");
        this.resolvedAt = resolvedAt;
        this.cancelledAt = cancelledAt;
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.method = Objects.requireNonNull(method, "method cannot be null");

        validateExpiration(createdAt, expiresAt);
        validateTokenRequirement(method, tokenHash);
        validateResolvedState(status, resolvedAt);
        validateCancelledState(status, cancelledAt);
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

    public static PasswordRecoveryRequest createEmailTokenRequest(
            Long userId,
            String tokenHash,
            Instant createdAt,
            Instant expiresAt
    ) {
        return new PasswordRecoveryRequest(
                null,
                userId,
                tokenHash,
                createdAt,
                expiresAt,
                null,
                null,
                PasswordRecoveryRequestStatus.OPEN,
                PasswordRecoveryRequestMethod.EMAIL_TOKEN
        );
    }

    public static PasswordRecoveryRequest createAdminResetLinkRequest(
            Long userId,
            String tokenHash,
            Instant createdAt,
            Instant expiresAt
    ) {
        return new PasswordRecoveryRequest(
                null,
                userId,
                tokenHash,
                createdAt,
                expiresAt,
                null,
                null,
                PasswordRecoveryRequestStatus.OPEN,
                PasswordRecoveryRequestMethod.ADMIN_RESET_LINK
        );
    }

    public static PasswordRecoveryRequest createAdminTemporaryPasswordRequest(
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
                PasswordRecoveryRequestMethod.ADMIN_TEMPORARY_PASSWORD
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

    public void defineAdminTemporaryPasswordMethod(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        if (!isOpen(now)) {
            throw new IllegalStateException("Password recovery request is not open or has expired.");
        }

        if (this.method != PasswordRecoveryRequestMethod.UNDEFINED) {
            throw new IllegalStateException("Password recovery method has already been defined.");
        }

        this.method = PasswordRecoveryRequestMethod.ADMIN_TEMPORARY_PASSWORD;
    }

    public void changeToAdminResetLinkMethod(
            String tokenHash,
            Instant now
    ) {
        Objects.requireNonNull(now, "now cannot be null");

        if (!isOpen(now)) {
            throw new IllegalStateException("Password recovery request is not open or has expired.");
        }

        if (tokenHash == null || tokenHash.isBlank()) {
            throw new IllegalArgumentException("tokenHash cannot be null or blank.");
        }

        this.method = PasswordRecoveryRequestMethod.ADMIN_RESET_LINK;
        this.tokenHash = tokenHash;
    }

    public boolean isOpen(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        return status == PasswordRecoveryRequestStatus.OPEN
                && !isExpired(now);
    }

    public boolean isExpired(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        return now.isAfter(expiresAt);
    }

    public boolean isTokenBased() {
        return method == PasswordRecoveryRequestMethod.EMAIL_TOKEN
                || method == PasswordRecoveryRequestMethod.ADMIN_RESET_LINK;
    }

    public boolean isTemporaryPasswordBased() {
        return method == PasswordRecoveryRequestMethod.ADMIN_TEMPORARY_PASSWORD;
    }


    public void resolve(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        if (!isOpen(now)) {
            throw new IllegalStateException("Password recovery request is not open or has expired.");
        }

        this.status = PasswordRecoveryRequestStatus.RESOLVED;
        this.resolvedAt = now;
    }

    public boolean canBeCancelled() {
        return status == PasswordRecoveryRequestStatus.OPEN;
    }

    public void cancel(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        if (!canBeCancelled()) {
            return;
        }

        this.status = PasswordRecoveryRequestStatus.CANCELLED;
        this.cancelledAt = now;
    }

    private static Long validateUserId(Long userId) {
        Objects.requireNonNull(userId, "userId cannot be null");

        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive.");
        }

        return userId;
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

        if (tokenRequired && (tokenHash == null || tokenHash.isBlank())) {
            throw new IllegalArgumentException("tokenHash is required for token-based password recovery requests.");
        }

        if (method == PasswordRecoveryRequestMethod.ADMIN_TEMPORARY_PASSWORD && tokenHash != null) {
            throw new IllegalArgumentException("temporary password recovery request cannot have tokenHash.");
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