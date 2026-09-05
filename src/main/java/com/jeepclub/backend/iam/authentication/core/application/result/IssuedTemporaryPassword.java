package com.jeepclub.backend.iam.authentication.core.application.result;

public record IssuedTemporaryPassword(
        String rawPassword,
        String passwordHash
) {
}
