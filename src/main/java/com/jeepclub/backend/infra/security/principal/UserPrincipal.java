package com.jeepclub.backend.infra.security.principal;

import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

@Getter
public class UserPrincipal {

    private final Long userId;
    private final Long sessionId;
    private final Instant accessTokenExpiresAt;

    public UserPrincipal(
            Long userId,
            Long sessionId,
            Instant accessTokenExpiresAt
    ) {
        this.userId = Objects.requireNonNull(userId, "userId is required");
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId is required");
        this.accessTokenExpiresAt = Objects.requireNonNull(
                accessTokenExpiresAt,
                "accessTokenExpiresAt is required"
        );
    }
}