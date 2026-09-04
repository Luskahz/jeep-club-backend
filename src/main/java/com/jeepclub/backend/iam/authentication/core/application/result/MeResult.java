package com.jeepclub.backend.iam.authentication.core.application.result;

public record MeResult(
        Long userId,
        Long sessionId,
        boolean sessionActive,
        long expiresInSeconds
) {
}
