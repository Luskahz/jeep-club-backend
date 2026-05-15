package com.jeepclub.backend.infra.web.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

public record ApiErrorResponse(
        String code,
        String message,
        int status,
        LocalDateTime timestamp
) {
    public static ApiErrorResponse of(
            String code,
            String message,
            HttpStatus status
    ) {
        return new ApiErrorResponse(
                code,
                message,
                status.value(),
                LocalDateTime.now()
        );
    }
}