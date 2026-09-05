package com.jeepclub.backend.iam.identity.api.http.exception;

import com.jeepclub.backend.iam.identity.api.module.exception.UserAlreadyDisabledException;
import com.jeepclub.backend.iam.identity.api.module.exception.UserNotDisabledException;
import com.jeepclub.backend.iam.identity.api.module.exception.UserNotFoundException;
import com.jeepclub.backend.iam.identity.api.module.exception.UserRegistrationConflictException;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.identity")
public class IdentityUserExceptionHandler extends ApiExceptionHandler {
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
}
