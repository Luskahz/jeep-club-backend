package com.jeepclub.backend.authentication.core.application.results;

import com.jeepclub.backend.authentication.core.domain.enums.PasswordRecoveryRequestMethod;
import com.jeepclub.backend.authentication.core.domain.enums.PasswordRecoveryRequestStatus;
import com.jeepclub.backend.authentication.core.domain.model.PasswordRecoveryRequest;

import java.time.Instant;

public record PublicPasswordRecoveryResult(
        PasswordRecoveryRequestStatus status,
        PasswordRecoveryRequestMethod method,
        Instant createdAt,
        Instant expiresAt,
        Instant resolvedAt,
        Instant cancelledAt
) {

    public static PublicPasswordRecoveryResult from(
            PasswordRecoveryRequest request
    ) {
        return new PublicPasswordRecoveryResult(
                request.getStatus(),
                request.getMethod(),
                request.getCreatedAt(),
                request.getExpiresAt(),
                request.getResolvedAt(),
                request.getCancelledAt()
        );
    }

    public static PublicPasswordRecoveryResult pending(
            Instant now,
            Instant expiresAt
    ) {
        return new PublicPasswordRecoveryResult(
                PasswordRecoveryRequestStatus.OPEN,
                PasswordRecoveryRequestMethod.UNDEFINED,
                now,
                expiresAt,
                null,
                null
        );
    }
}