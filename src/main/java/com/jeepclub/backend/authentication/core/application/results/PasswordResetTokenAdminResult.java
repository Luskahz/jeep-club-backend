package com.jeepclub.backend.authentication.core.application.results;

import java.util.Objects;

public record PasswordResetTokenAdminResult(
        String resetToken
) {
    public PasswordResetTokenAdminResult {
        Objects.requireNonNull(resetToken, "resetToken cannot be null");
    }
}