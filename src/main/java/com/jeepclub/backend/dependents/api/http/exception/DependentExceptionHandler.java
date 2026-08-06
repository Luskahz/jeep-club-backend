package com.jeepclub.backend.dependents.api.http.exception;

import com.jeepclub.backend.dependents.core.domain.exception.DependentException;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.dependents.api")
public class DependentExceptionHandler extends ApiExceptionHandler {

    @ExceptionHandler(DependentException.class)
    public ResponseEntity<ApiErrorResponse> handleDependentException(DependentException exception) {
        return switch (exception.getViolation()) {
            case NOT_FOUND -> buildErrorResponse(
                    "DEPENDENT_NOT_FOUND",
                    exception.getMessage(),
                    HttpStatus.NOT_FOUND
            );
            case ACCESS_DENIED -> buildErrorResponse(
                    "DEPENDENT_ACCESS_DENIED",
                    exception.getMessage(),
                    HttpStatus.FORBIDDEN
            );
            case CONFLICT -> buildErrorResponse(
                    "DEPENDENT_CONFLICT",
                    exception.getMessage(),
                    HttpStatus.CONFLICT
            );
            case BUSINESS_RULE -> buildErrorResponse(
                    "DEPENDENT_BUSINESS_RULE_VIOLATION",
                    exception.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        };
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException exception
    ) {
        return buildErrorResponse(
                "INVALID_ARGUMENT",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }
}
