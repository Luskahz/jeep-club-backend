package com.jeepclub.backend.authorization.api.http.exception;

import com.jeepclub.backend.authorization.core.application.exception.RolePermissionAlreadyExistsException;
import com.jeepclub.backend.authorization.core.application.exception.RolePermissionNotFoundException;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.authorization.api")
public class RolePermissionExceptionHandler extends ApiExceptionHandler {

    @ExceptionHandler(RolePermissionAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleRolePermissionAlreadyExists(
            RolePermissionAlreadyExistsException exception
    ) {
        return buildErrorResponse(
                "ROLE_PERMISSION_ALREADY_EXISTS",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(RolePermissionNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleRolePermissionNotFound(
            RolePermissionNotFoundException exception
    ) {
        return buildErrorResponse(
                "ROLE_PERMISSION_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }
}
