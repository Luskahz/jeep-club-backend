package com.jeepclub.backend.authentication.core.application.results.admin.recovery;

import com.jeepclub.backend.authentication.core.domain.model.PasswordResetRequest;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record AdminPasswordRecoveryRequestResult(
        Long id,
        Long userId,
        String status,
        String origin,
        Instant createdAt,
        Instant expiresAt,
        Instant resolvedAt,
        Instant cancelledAt
) {

    public static AdminPasswordRecoveryRequestResult from(PasswordResetRequest request) {
        Objects.requireNonNull(request, "request cannot be null");

        return new AdminPasswordRecoveryRequestResult(
                request.getId(),
                request.getUserId(),
                request.getStatus().name(),
                request.getOrigin() == null ? null : request.getOrigin().name(),
                request.getCreatedAt(),
                request.getExpiresAt(),
                request.getResolvedAt(),
                request.getCancelledAt()
        );
    }

    public static List<AdminPasswordRecoveryRequestResult> from(List<PasswordResetRequest> requests) {
        Objects.requireNonNull(requests, "requests cannot be null");

        return requests.stream()
                .map(AdminPasswordRecoveryRequestResult::from)
                .toList();
    }
}