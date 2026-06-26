package com.jeepclub.backend.authentication.core.application.result;

public record IssuedTemporaryPassword(
        String rawPassword,
        String passwordHash
) {
}
