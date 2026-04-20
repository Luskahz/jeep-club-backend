package com.jeepclub.backend.infra.security.jwt;

import java.time.Instant;

public record JwtAuthenticatedUser(
        Long userId,
        Long sessionId,
        Instant expiresAt
) {}
