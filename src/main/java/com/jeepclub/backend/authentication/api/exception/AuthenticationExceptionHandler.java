package com.jeepclub.backend.authentication.api.exception;

import com.jeepclub.backend.infra.web.exception.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public abstract class AuthenticationExceptionHandler {

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