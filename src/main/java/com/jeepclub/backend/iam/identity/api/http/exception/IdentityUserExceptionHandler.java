package com.jeepclub.backend.iam.identity.api.http.exception;

import com.jeepclub.backend.iam.identity.core.application.exception.AdministrativeUserUpdateConflictException;
import com.jeepclub.backend.iam.identity.api.module.exception.RootUserCannotBeDisabledException;
import com.jeepclub.backend.iam.identity.api.module.exception.UserAlreadyDisabledException;
import com.jeepclub.backend.iam.identity.api.module.exception.UserNotDisabledException;
import com.jeepclub.backend.iam.identity.api.module.exception.UserNotFoundException;
import com.jeepclub.backend.iam.identity.api.module.exception.UserRegistrationConflictException;
import com.jeepclub.backend.iam.identity.api.module.exception.UserEmailAlreadyInUseException;
import com.jeepclub.backend.iam.identity.core.application.exception.UserRgAlreadyInUseException;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.iam.identity")
public class IdentityUserExceptionHandler extends ApiExceptionHandler {
    @ExceptionHandler(com.jeepclub.backend.iam.identity.api.module.exception.UserAuthenticationLifecycleConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationLifecycleConflict(RuntimeException exception) {
        return buildErrorResponse("USER_AUTHENTICATION_LIFECYCLE_CONFLICT", exception.getMessage(), HttpStatus.CONFLICT);
    }
    @ExceptionHandler(com.jeepclub.backend.iam.identity.api.module.exception.UserDeletionConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleDeletionConflict(com.jeepclub.backend.iam.identity.api.module.exception.UserDeletionConflictException exception) {
        return buildErrorResponse("USER_DELETION_CONFLICT", exception.getMessage(), HttpStatus.CONFLICT);
    }
    @ExceptionHandler(com.jeepclub.backend.iam.identity.core.domain.exception.UserLifecycleConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleLifecycleConflict(com.jeepclub.backend.iam.identity.core.domain.exception.UserLifecycleConflictException exception) {
        return buildErrorResponse("USER_LIFECYCLE_CONFLICT", exception.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(com.jeepclub.backend.iam.identity.api.module.exception.UserIdentityChangeConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleIdentityChangeConflict(RuntimeException exception) {
        return buildErrorResponse("USER_IDENTITY_CHANGE_CONFLICT", exception.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.springframework.dao.ConcurrencyFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleConcurrencyConflict(RuntimeException exception) {
        return buildErrorResponse("USER_CONCURRENT_MODIFICATION", "Concurrent user modification; retry the operation.", HttpStatus.CONFLICT);
    }
    @ExceptionHandler(AdministrativeUserUpdateConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleAdministrativeUpdateConflict(RuntimeException exception) {
        return buildErrorResponse("USER_UPDATE_CONFLICT", exception.getMessage(), HttpStatus.CONFLICT);
    }
    @ExceptionHandler(UserRgAlreadyInUseException.class)
    public ResponseEntity<ApiErrorResponse> handleRgAlreadyInUse(UserRgAlreadyInUseException exception) {
        return buildErrorResponse("USER_RG_ALREADY_IN_USE", exception.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(UserNotFoundException exception) {
        return buildErrorResponse("USER_ID_NOT_FOUND", exception.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserAlreadyDisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleAlreadyDisabled(UserAlreadyDisabledException exception) {
        return buildErrorResponse("USER_ALREADY_DISABLED", exception.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UserNotDisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleNotDisabled(UserNotDisabledException exception) {
        return buildErrorResponse("USER_NOT_DISABLED", exception.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UserRegistrationConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleRegistrationConflict(UserRegistrationConflictException exception) {
        return buildErrorResponse("REGISTRATION_CONFLICT", exception.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UserEmailAlreadyInUseException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailAlreadyInUse(UserEmailAlreadyInUseException exception) {
        return buildErrorResponse("USER_EMAIL_ALREADY_IN_USE", exception.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(RootUserCannotBeDisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleRootUserCannotBeDisabled(RootUserCannotBeDisabledException exception) {
        return buildErrorResponse("USER_ROOT_CANNOT_BE_DISABLED", exception.getMessage(), HttpStatus.CONFLICT);
    }
}
