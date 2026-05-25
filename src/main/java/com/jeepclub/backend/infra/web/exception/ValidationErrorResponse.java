package com.jeepclub.backend.infra.web.exception;

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