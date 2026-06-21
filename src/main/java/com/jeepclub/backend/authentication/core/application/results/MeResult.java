package com.jeepclub.backend.authentication.core.application.results;

public record MeResult(
        Long userId,
        String userName,
        Long sessionId,
        boolean sessionActive,
        long expiresInSeconds
) {
}