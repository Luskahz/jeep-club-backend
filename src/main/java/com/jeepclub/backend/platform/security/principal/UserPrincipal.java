package com.jeepclub.backend.platform.security.principal;

import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

@Getter
public class UserPrincipal {

    private final Long userId;
    private final Long sessionId;
    /**
     * Human-readable, non-authoritative context from the access-token {@code name} claim.
     * It is {@code null} while a valid legacy token without that claim remains active.
     */
    private final String userName;
    private final Instant accessTokenExpiresAt;

    public UserPrincipal(
            Long userId,
            Long sessionId,
            String userName,
            Instant accessTokenExpiresAt
    ) {
        this.userId = Objects.requireNonNull(userId, "userId is required");
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId is required");
        this.userName = userName;
        this.accessTokenExpiresAt = Objects.requireNonNull(
                accessTokenExpiresAt,
                "accessTokenExpiresAt is required"
        );
    }
}
