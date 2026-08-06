package com.jeepclub.backend.memberships.core.port;

public record PendingFirstAccessUser(
        Long userId,
        String temporaryPassword
) {

    public PendingFirstAccessUser {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        if (temporaryPassword == null || temporaryPassword.isBlank()) {
            throw new IllegalArgumentException("temporaryPassword must not be blank");
        }
    }
}
