package com.jeepclub.backend.platform.web.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public abstract class ApiExceptionHandler {

    protected final ResponseEntity<ApiErrorResponse> buildErrorResponse(
            String code,
            String message,
            HttpStatus status
    ) {
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(
                        code,
                        message,
                        status
                ));
    }
}