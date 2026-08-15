package com.jeepclub.backend.authorization.api.http.exception;

import com.jeepclub.backend.authorization.core.application.exception.permission.PermissionNotFoundException;
import com.jeepclub.backend.authorization.core.domain.exception.permission.PermissionCodeMismatchException;
import com.jeepclub.backend.authorization.core.domain.exception.permission.PermissionDescriptionCannotBeBlankException;
import com.jeepclub.backend.authorization.core.domain.exception.permission.PermissionDescriptionTooLongException;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.authorization.api")
public class PermissionExceptionHandler extends ApiExceptionHandler {

    @ExceptionHandler(PermissionNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handlePermissionNotFound(
            PermissionNotFoundException exception
    ) {
        return buildErrorResponse(
                "PERMISSION_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler({
            PermissionDescriptionCannotBeBlankException.class,
            PermissionDescriptionTooLongException.class,
            PermissionCodeMismatchException.class
    })
    public ResponseEntity<ApiErrorResponse> handleInvalidPermission(RuntimeException exception) {
        return buildErrorResponse(
                "PERMISSION_INVALID_DATA",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }
}
