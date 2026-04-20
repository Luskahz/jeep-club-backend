package com.jeepclub.backend.authentication.core.application.results;

public record MeResponse(
        Long userId,
        Long sessionId,
        boolean sessionActive,
        long expiresInSeconds
) {
}
