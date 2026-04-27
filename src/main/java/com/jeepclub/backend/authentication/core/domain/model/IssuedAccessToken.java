package com.jeepclub.backend.authentication.core.domain.model;

import java.time.Instant;

public record IssuedAccessToken(
        String token,
        Instant expiresAt
) {}
