package com.jeepclub.backend.authorization.api.exception;

import com.jeepclub.backend.authorization.core.application.exception.PermissionNotFoundException;
import com.jeepclub.backend.infra.web.exception.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.authorization.api")
public class AuthorizationExceptionHandler {

    @ExceptionHandler(PermissionNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handlePermissionNotFound(
            PermissionNotFoundException exception
    ) {
        HttpStatus status = HttpStatus.NOT_FOUND;

        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(
                        "PERMISSION_NOT_FOUND",
                        exception.getMessage(),
                        status
                ));
    }
}