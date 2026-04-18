package com.jeepclub.backend.authentication.core.services;

public record MeResponse(
        Long userId,
        Long sessionId,
        boolean sessionActive,
        long expiresInSeconds
) {
}
