package com.jeepclub.backend.authentication.core.application.result;

import com.jeepclub.backend.authentication.core.domain.model.PasswordRecoveryRequest;

import java.util.Objects;

public record PasswordResetLinkAdminResult(
        String resetLink,
        PasswordRecoveryRequest recoveryRequest
) {

    public PasswordResetLinkAdminResult {
        if (resetLink == null || resetLink.isBlank()) {
            throw new IllegalArgumentException("resetLink cannot be null or blank");
        }

        Objects.requireNonNull(recoveryRequest, "recoveryRequest cannot be null");
    }
}