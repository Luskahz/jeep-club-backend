package com.jeepclub.backend.authentication.core.domain.model;

import com.jeepclub.backend.authentication.core.domain.enums.PasswordRecoveryRequestMethod;
import com.jeepclub.backend.authentication.core.domain.enums.PasswordRecoveryRequestStatus;
import com.jeepclub.backend.authentication.core.domain.exception.passwordrecovery.PasswordRecoveryRequestStateException;
import com.jeepclub.backend.authentication.core.domain.exception.passwordrecovery.PasswordRecoveryRequestValidationException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PasswordRecoveryRequest {

    private static final int MAX_TOKEN_HASH_LENGTH = 128;

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
        this.createdAt = requireValue(
                createdAt,
                "createdAt"
        );
        this.expiresAt = requireValue(
                expiresAt,
                "expiresAt"
        );
        this.resolvedAt = resolvedAt;
        this.cancelledAt = cancelledAt;
        this.status = requireValue(
                status,
                "status"
        );
        this.method = requireValue(
                method,
                "method"
        );

        validateExpiration(
                this.createdAt,
                this.expiresAt
        );

        validateTokenRequirement(
                this.method,
                this.tokenHash
        );

        validateResolvedState(
                this.status,
                this.method,
                this.createdAt,
                this.expiresAt,
                this.resolvedAt
        );

        validateCancelledState(
                this.status,
                this.createdAt,
                this.cancelledAt
        );
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
        validateId(id);

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
        Instant validatedNow = requireNow(now);

        assertOpen(validatedNow);

        String normalizedTokenHash =
                validateRequiredTokenHash(tokenHash);

        this.method =
                PasswordRecoveryRequestMethod.EMAIL_TOKEN;

        this.tokenHash = normalizedTokenHash;
    }

    public void changeToAdminResetLinkMethod(
            String tokenHash,
            Instant now
    ) {
        Instant validatedNow = requireNow(now);

        assertOpen(validatedNow);

        String normalizedTokenHash =
                validateRequiredTokenHash(tokenHash);

        this.method =
                PasswordRecoveryRequestMethod.ADMIN_RESET_LINK;

        this.tokenHash = normalizedTokenHash;
    }

    public void changeToAdminTemporaryPasswordMethod(
            Instant now
    ) {
        Instant validatedNow = requireNow(now);

        assertOpen(validatedNow);

        this.method =
                PasswordRecoveryRequestMethod.ADMIN_TEMPORARY_PASSWORD;

        this.tokenHash = null;
    }

    public void resolve(Instant now) {
        Instant validatedNow = requireNow(now);

        assertOpen(validatedNow);

        if (hasUndefinedMethod()) {
            throw new PasswordRecoveryRequestStateException(
                    "Password recovery request method must be defined before resolution."
            );
        }

        this.status =
                PasswordRecoveryRequestStatus.RESOLVED;

        this.resolvedAt = validatedNow;
    }

    public void cancel(Instant now) {
        Instant validatedNow = requireNow(now);

        if (!canBeCancelled()) {
            return;
        }

        this.status =
                PasswordRecoveryRequestStatus.CANCELLED;

        this.cancelledAt = validatedNow;
    }

    public boolean isOpen(Instant now) {
        Instant validatedNow = requireNow(now);

        return status == PasswordRecoveryRequestStatus.OPEN
                && !isExpired(validatedNow);
    }

    public boolean isExpired(Instant now) {
        Instant validatedNow = requireNow(now);

        return !validatedNow.isBefore(expiresAt);
    }

    public boolean canBeCancelled() {
        return status == PasswordRecoveryRequestStatus.OPEN;
    }

    public boolean isTokenBased() {
        return method == PasswordRecoveryRequestMethod.EMAIL_TOKEN
                || method == PasswordRecoveryRequestMethod.ADMIN_RESET_LINK;
    }

    public boolean isTemporaryPasswordBased() {
        return method
                == PasswordRecoveryRequestMethod.ADMIN_TEMPORARY_PASSWORD;
    }

    public boolean hasUndefinedMethod() {
        return method
                == PasswordRecoveryRequestMethod.UNDEFINED;
    }

    private void assertOpen(Instant now) {
        if (!isOpen(now)) {
            throw new PasswordRecoveryRequestStateException(
                    "Password recovery request is not open or has expired."
            );
        }
    }

    private static void validateId(Long id) {
        if (id == null) {
            throw new PasswordRecoveryRequestValidationException(
                    "id cannot be null."
            );
        }

        if (id <= 0) {
            throw new PasswordRecoveryRequestValidationException(
                    "id must be positive."
            );
        }
    }

    private static Long validateUserId(Long userId) {
        if (userId == null) {
            throw new PasswordRecoveryRequestValidationException(
                    "userId cannot be null."
            );
        }

        if (userId <= 0) {
            throw new PasswordRecoveryRequestValidationException(
                    "userId must be positive."
            );
        }

        return userId;
    }

    private static Instant requireNow(Instant now) {
        return requireValue(
                now,
                "now"
        );
    }

    private static <T> T requireValue(
            T value,
            String fieldName
    ) {
        if (value == null) {
            throw new PasswordRecoveryRequestValidationException(
                    fieldName + " cannot be null."
            );
        }

        return value;
    }

    private static String normalizeTokenHash(
            String tokenHash
    ) {
        if (tokenHash == null) {
            return null;
        }

        String normalizedTokenHash =
                tokenHash.trim();

        if (normalizedTokenHash.isBlank()) {
            return null;
        }

        if (
                normalizedTokenHash.length()
                        > MAX_TOKEN_HASH_LENGTH
        ) {
            throw new PasswordRecoveryRequestValidationException(
                    "tokenHash cannot exceed "
                            + MAX_TOKEN_HASH_LENGTH
                            + " characters."
            );
        }

        return normalizedTokenHash;
    }

    private static String validateRequiredTokenHash(
            String tokenHash
    ) {
        String normalizedTokenHash =
                normalizeTokenHash(tokenHash);

        if (normalizedTokenHash == null) {
            throw new PasswordRecoveryRequestValidationException(
                    "tokenHash cannot be null or blank."
            );
        }

        return normalizedTokenHash;
    }

    private static void validateExpiration(
            Instant createdAt,
            Instant expiresAt
    ) {
        if (!expiresAt.isAfter(createdAt)) {
            throw new PasswordRecoveryRequestValidationException(
                    "expiresAt must be after createdAt."
            );
        }
    }

    private static void validateTokenRequirement(
            PasswordRecoveryRequestMethod method,
            String tokenHash
    ) {
        boolean tokenRequired =
                method == PasswordRecoveryRequestMethod.EMAIL_TOKEN
                        || method
                        == PasswordRecoveryRequestMethod.ADMIN_RESET_LINK;

        if (tokenRequired && tokenHash == null) {
            throw new PasswordRecoveryRequestValidationException(
                    "tokenHash is required for token-based password recovery requests."
            );
        }

        boolean tokenForbidden =
                method == PasswordRecoveryRequestMethod.UNDEFINED
                        || method
                        == PasswordRecoveryRequestMethod.ADMIN_TEMPORARY_PASSWORD;

        if (tokenForbidden && tokenHash != null) {
            throw new PasswordRecoveryRequestValidationException(
                    "tokenHash must be null for non-token password recovery requests."
            );
        }
    }

    private static void validateResolvedState(
            PasswordRecoveryRequestStatus status,
            PasswordRecoveryRequestMethod method,
            Instant createdAt,
            Instant expiresAt,
            Instant resolvedAt
    ) {
        if (
                status
                        == PasswordRecoveryRequestStatus.RESOLVED
        ) {
            if (resolvedAt == null) {
                throw new PasswordRecoveryRequestValidationException(
                        "resolvedAt is required when status is RESOLVED."
                );
            }

            if (method == PasswordRecoveryRequestMethod.UNDEFINED) {
                throw new PasswordRecoveryRequestValidationException(
                        "method must be defined when status is RESOLVED."
                );
            }

            if (resolvedAt.isBefore(createdAt)) {
                throw new PasswordRecoveryRequestValidationException(
                        "resolvedAt cannot be before createdAt."
                );
            }

            if (!resolvedAt.isBefore(expiresAt)) {
                throw new PasswordRecoveryRequestValidationException(
                        "resolvedAt must be before expiresAt."
                );
            }

            return;
        }

        if (resolvedAt != null) {
            throw new PasswordRecoveryRequestValidationException(
                    "resolvedAt must be null when status is not RESOLVED."
            );
        }
    }

    private static void validateCancelledState(
            PasswordRecoveryRequestStatus status,
            Instant createdAt,
            Instant cancelledAt
    ) {
        if (
                status
                        == PasswordRecoveryRequestStatus.CANCELLED
        ) {
            if (cancelledAt == null) {
                throw new PasswordRecoveryRequestValidationException(
                        "cancelledAt is required when status is CANCELLED."
                );
            }

            if (cancelledAt.isBefore(createdAt)) {
                throw new PasswordRecoveryRequestValidationException(
                        "cancelledAt cannot be before createdAt."
                );
            }

            return;
        }

        if (cancelledAt != null) {
            throw new PasswordRecoveryRequestValidationException(
                    "cancelledAt must be null when status is not CANCELLED."
            );
        }
    }
}