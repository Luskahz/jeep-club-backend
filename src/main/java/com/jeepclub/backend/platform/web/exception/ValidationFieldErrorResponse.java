package com.jeepclub.backend.platform.web.exception;

public record ValidationFieldErrorResponse(
        String field,
        String code,
        String message
) {
}
