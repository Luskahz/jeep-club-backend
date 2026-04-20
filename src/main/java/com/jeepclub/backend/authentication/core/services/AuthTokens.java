package com.jeepclub.backend.authentication.core.services;

public record AuthTokens(
        String refreshToken,
        String accessToken,
        long expiresInSeconds
) {}