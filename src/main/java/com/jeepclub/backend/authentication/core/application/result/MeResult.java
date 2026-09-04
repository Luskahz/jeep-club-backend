package com.jeepclub.backend.authentication.core.application.result;

public record MeResult(
        Long userId,
        Long sessionId,
        boolean sessionActive,
        long expiresInSeconds
) {
}
