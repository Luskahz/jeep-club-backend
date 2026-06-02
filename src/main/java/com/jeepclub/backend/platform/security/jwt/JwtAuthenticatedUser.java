package com.jeepclub.backend.platform.security.jwt;

import java.time.Instant;

public record JwtAuthenticatedUser(
        Long userId,
        Long sessionId,
        Instant expiresAt
) {
}