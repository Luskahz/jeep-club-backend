package com.jeepclub.backend.authentication.core.application.results;

public record PasswordRecoveryAdminResult(
        String temporaryPassword,
        String resetToken
) {
}
