package com.jeepclub.backend.authorization.api.exception;

import com.jeepclub.backend.authorization.core.application.exception.PermissionNotFoundException;
import com.jeepclub.backend.authorization.core.application.exception.RoleAlreadyExistsException;
import com.jeepclub.backend.authorization.core.application.exception.RoleNotFoundException;
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
        return buildErrorResponse(
                "PERMISSION_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleRoleNotFound(
            RoleNotFoundException exception
    ) {
        return buildErrorResponse(
                "ROLE_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(RoleAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleRoleAlreadyExists(
            RoleAlreadyExistsException exception
    ) {
        return buildErrorResponse(
                "ROLE_ALREADY_EXISTS",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

//    @ExceptionHandler(RoleAlreadyDeletedException.class)
//    public ResponseEntity<ApiErrorResponse> handleRoleAlreadyDeleted(
//            RoleAlreadyDeletedException exception
//    ) {
//        return buildErrorResponse(
//                "ROLE_ALREADY_DELETED",
//                exception.getMessage(),
//                HttpStatus.CONFLICT
//        );
//    }

//    @ExceptionHandler(RoleInactiveException.class)
//    public ResponseEntity<ApiErrorResponse> handleRoleInactive(
//            RoleInactiveException exception
//    ) {
//        return buildErrorResponse(
//                "ROLE_INACTIVE",
//                exception.getMessage(),
//                HttpStatus.CONFLICT
//        );
//    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(
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