package com.jeepclub.backend.authentication.core.application.results;

import com.jeepclub.backend.authentication.core.domain.model.PasswordRecoveryRequest;

import java.util.Objects;

public record TemporaryPasswordAdminResult(
        String temporaryPassword,
        PasswordRecoveryRequest recoveryRequest
) {

    public TemporaryPasswordAdminResult {
        if (temporaryPassword == null || temporaryPassword.isBlank()) {
            throw new IllegalArgumentException("temporaryPassword cannot be null or blank");
        }

        Objects.requireNonNull(recoveryRequest, "recoveryRequest cannot be null");
    }
}