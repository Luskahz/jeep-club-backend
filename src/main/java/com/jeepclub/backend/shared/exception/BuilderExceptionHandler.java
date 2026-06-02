package com.jeepclub.backend.shared.exception;

import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public abstract class BuilderExceptionHandler {

    protected ResponseEntity<ApiErrorResponse> buildErrorResponse(
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