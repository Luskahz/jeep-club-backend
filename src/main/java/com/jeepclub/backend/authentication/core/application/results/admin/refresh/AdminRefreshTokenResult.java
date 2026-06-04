package com.jeepclub.backend.authentication.core.application.results.admin.refresh;

import com.jeepclub.backend.authentication.core.domain.model.RefreshToken;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record AdminRefreshTokenResult(
        Long id,
        Long userId,
        Long sessionId,
        String status,
        Instant createdAt,
        Instant expiresAt,
        Instant revokedAt
) {

    public static AdminRefreshTokenResult from(RefreshToken refreshToken) {
        Objects.requireNonNull(refreshToken, "refreshToken cannot be null");

        return new AdminRefreshTokenResult(
                refreshToken.getId(),
                refreshToken.getUserId(),
                refreshToken.getSessionId(),
                refreshToken.getStatus().name(),
                refreshToken.getCreatedAt(),
                refreshToken.getExpiresAt(),
                refreshToken.getRevokedAt()
        );
    }

    public static List<AdminRefreshTokenResult> from(List<RefreshToken> refreshTokens) {
        Objects.requireNonNull(refreshTokens, "refreshTokens cannot be null");

        return refreshTokens.stream()
                .map(AdminRefreshTokenResult::from)
                .toList();
    }
}