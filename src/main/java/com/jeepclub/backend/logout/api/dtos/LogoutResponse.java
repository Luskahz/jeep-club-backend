package com.jeepclub.backend.authentication.api.dtos.logout;

import java.time.Instant;

public record LogoutResponse(
        String message,
        Instant revokedAt
) {
    public static LogoutResponse success() {
        return new LogoutResponse("Session successfully revoked.", Instant.now());
    }
}
