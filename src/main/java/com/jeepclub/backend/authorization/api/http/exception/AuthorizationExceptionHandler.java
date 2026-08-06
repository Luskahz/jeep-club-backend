package com.jeepclub.backend.authorization.api.http.exception;

import com.jeepclub.backend.authorization.core.application.exception.PermissionNotFoundException;
import com.jeepclub.backend.authorization.core.application.exception.RoleAlreadyExistsException;
import com.jeepclub.backend.authorization.core.application.exception.RoleNotFoundException;
import com.jeepclub.backend.authorization.core.application.exception.RolePermissionAlreadyExistsException;
import com.jeepclub.backend.authorization.core.application.exception.RolePermissionNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserIdNotFoundException;
import com.jeepclub.backend.authorization.core.application.exception.UserRoleAlreadyExistsException;
import com.jeepclub.backend.authorization.core.application.exception.UserRoleNotFoundException;
import com.jeepclub.backend.authorization.core.domain.exception.role.DeletedRoleCannotBeChangedException;
import com.jeepclub.backend.authorization.core.domain.exception.role.InactiveRoleCannotBeUsedException;
import com.jeepclub.backend.authorization.core.domain.exception.permission.PermissionCodeMismatchException;
import com.jeepclub.backend.authorization.core.domain.exception.permission.PermissionDescriptionCannotBeBlankException;
import com.jeepclub.backend.authorization.core.domain.exception.permission.PermissionDescriptionTooLongException;
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
public class AuthorizationExceptionHandler extends ApiExceptionHandler {

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

    @ExceptionHandler(UserIdNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFound(
            UserIdNotFoundException exception
    ) {
        return buildErrorResponse(
                "USER_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
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
