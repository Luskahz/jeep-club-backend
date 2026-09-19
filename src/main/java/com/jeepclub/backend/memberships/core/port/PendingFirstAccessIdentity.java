package com.jeepclub.backend.memberships.core.port;

public record PendingFirstAccessIdentity(Long userId) {
    public PendingFirstAccessIdentity {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
    }
}
