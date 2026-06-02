package com.jeepclub.backend.platform.web.exception;

import java.time.Instant;
import java.util.List;

public record ValidationErrorResponse(
        Instant timestamp,
        String code,
        String message,
        int status,
        List<ValidationFieldErrorResponse> errors
) {
}