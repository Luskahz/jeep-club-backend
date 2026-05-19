package com.jeepclub.backend.authentication.core.application.results;

import java.util.Objects;

public record TemporaryPasswordAdminResult(
        String temporaryPassword
) {
    public TemporaryPasswordAdminResult {
        Objects.requireNonNull(temporaryPassword, "temporaryPassword cannot be null");
    }
}