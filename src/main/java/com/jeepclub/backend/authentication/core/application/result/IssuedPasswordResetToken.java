package com.jeepclub.backend.authentication.core.application.result;

public record IssuedPasswordResetToken(
        String rawToken,
        String tokenHash,
        String resetLink
) {
}
