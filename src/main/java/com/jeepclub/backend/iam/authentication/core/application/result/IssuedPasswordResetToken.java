package com.jeepclub.backend.iam.authentication.core.application.result;

public record IssuedPasswordResetToken(
        String rawToken,
        String tokenHash,
        String resetLink
) {
}
