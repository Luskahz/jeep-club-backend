package com.jeepclub.backend.infra.web.exception;

public record ValidationFieldErrorResponse(
        String field,
        String message,
        Object rejectedValue
) {
}