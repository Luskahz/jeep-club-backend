package com.jeepclub.backend.iam.authentication.core.application.result.admin.recovery;

import com.jeepclub.backend.iam.authentication.core.domain.model.PasswordRecoveryRequest;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record AdminPasswordRecoveryRequestResult(
        Long id,
        Long userId,
        String status,
        String method,
        Instant createdAt,
        Instant expiresAt,
        Instant resolvedAt,
        Instant cancelledAt
) {

    public static AdminPasswordRecoveryRequestResult from(PasswordRecoveryRequest request) {
        Objects.requireNonNull(request, "request cannot be null");

        return new AdminPasswordRecoveryRequestResult(
                request.getId(),
                request.getUserId(),
                request.getStatus().name(),
                request.getMethod().name(),
                request.getCreatedAt(),
                request.getExpiresAt(),
                request.getResolvedAt(),
                request.getCancelledAt()
        );
    }

    public static List<AdminPasswordRecoveryRequestResult> from(List<PasswordRecoveryRequest> requests) {
        Objects.requireNonNull(requests, "requests cannot be null");

        return requests.stream()
                .map(AdminPasswordRecoveryRequestResult::from)
                .toList();
    }
}