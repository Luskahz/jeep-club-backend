package com.jeepclub.backend.iam.authentication.core.application.result.login;

import java.time.Instant;
import java.util.Objects;

public record PasswordChangeRequiredLoginResult(
        String passwordChangeToken,
        Instant expiresAt
) implements LoginResult {

    public PasswordChangeRequiredLoginResult {
        if (passwordChangeToken == null || passwordChangeToken.isBlank()) {
            throw new IllegalArgumentException("passwordChangeToken cannot be null or blank");
        }

        Objects.requireNonNull(expiresAt, "expiresAt cannot be null");
    }
}