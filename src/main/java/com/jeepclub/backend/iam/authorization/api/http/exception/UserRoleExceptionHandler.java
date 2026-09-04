package com.jeepclub.backend.iam.authorization.api.http.exception;

import com.jeepclub.backend.iam.authorization.core.application.exception.role.RootRoleCannotBeManagedManuallyException;
import com.jeepclub.backend.iam.authorization.core.application.exception.userrole.AuthorizationUserNotFoundException;
import com.jeepclub.backend.iam.authorization.core.application.exception.userrole.UserRoleAlreadyExistsException;
import com.jeepclub.backend.iam.authorization.core.application.exception.userrole.UserRoleNotFoundException;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.authorization.api")
public class UserRoleExceptionHandler extends ApiExceptionHandler {

    @ExceptionHandler(UserRoleAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleUserRoleAlreadyExists(
            UserRoleAlreadyExistsException exception
    ) {
        return buildErrorResponse(
                "USER_ROLE_ALREADY_EXISTS",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(UserRoleNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserRoleNotFound(
            UserRoleNotFoundException exception
    ) {
        return buildErrorResponse(
                "USER_ROLE_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(AuthorizationUserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFound(
            AuthorizationUserNotFoundException exception
    ) {
        return buildErrorResponse(
                "USER_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(RootRoleCannotBeManagedManuallyException.class)
    public ResponseEntity<ApiErrorResponse> handleRootRoleCannotBeManagedManually(
            RootRoleCannotBeManagedManuallyException exception
    ) {
        return buildErrorResponse(
                "ROOT_ROLE_CANNOT_BE_MANAGED_MANUALLY",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }
}
