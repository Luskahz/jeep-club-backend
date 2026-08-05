package com.jeepclub.backend.memberships.core.port;

public record PendingFirstAccessLink(
        Long userId,
        String accessLink
) {

    public PendingFirstAccessLink {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        if (accessLink == null || accessLink.isBlank()) {
            throw new IllegalArgumentException("accessLink must not be blank");
        }
    }
}
