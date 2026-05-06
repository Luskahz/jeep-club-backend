package com.jeepclub.backend.infra.security.principal;

import lombok.Getter;

import java.time.Instant;

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
        this.userId = userId;
        this.sessionId = sessionId;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
    }
}