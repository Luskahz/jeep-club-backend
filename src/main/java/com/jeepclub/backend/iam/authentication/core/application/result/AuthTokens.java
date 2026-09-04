package com.jeepclub.backend.iam.authentication.core.application.result;

public record AuthTokens(
        String refreshToken,
        String accessToken,
        long expiresInSeconds
) {}