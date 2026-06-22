package com.jeepclub.backend.authentication.core.application.result;

public record AuthTokens(
        String refreshToken,
        String accessToken,
        long expiresInSeconds
) {}