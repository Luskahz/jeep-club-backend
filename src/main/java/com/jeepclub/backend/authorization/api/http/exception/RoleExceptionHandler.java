package com.jeepclub.backend.authorization.api.http.exception;

import com.jeepclub.backend.authorization.core.application.exception.role.RoleAlreadyExistsException;
import com.jeepclub.backend.authorization.core.application.exception.role.RoleNotFoundException;
import com.jeepclub.backend.authorization.core.domain.exception.role.DeletedRoleCannotBeChangedException;
import com.jeepclub.backend.authorization.core.domain.exception.role.InactiveRoleCannotBeUsedException;
import com.jeepclub.backend.authorization.core.domain.exception.role.RoleDescriptionTooLongException;
import com.jeepclub.backend.authorization.core.domain.exception.role.RoleNameCannotBeBlankException;
import com.jeepclub.backend.authorization.core.domain.exception.role.RoleNameTooLongException;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.authorization.api")
public class RoleExceptionHandler extends ApiExceptionHandler {

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

    @ExceptionHandler(DeletedRoleCannotBeChangedException.class)
    public ResponseEntity<ApiErrorResponse> handleDeletedRoleCannotBeChanged(
            DeletedRoleCannotBeChangedException exception
    ) {
        return buildErrorResponse(
                "DELETED_ROLE_CANNOT_BE_CHANGED",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(InactiveRoleCannotBeUsedException.class)
    public ResponseEntity<ApiErrorResponse> handleInactiveRoleCannotBeUsed(
            InactiveRoleCannotBeUsedException exception
    ) {
        return buildErrorResponse(
                "INACTIVE_ROLE_CANNOT_BE_USED",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler({
            RoleNameCannotBeBlankException.class,
            RoleNameTooLongException.class,
            RoleDescriptionTooLongException.class
    })
    public ResponseEntity<ApiErrorResponse> handleInvalidRole(RuntimeException exception) {
        return buildErrorResponse(
                "ROLE_INVALID_DATA",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }
}
