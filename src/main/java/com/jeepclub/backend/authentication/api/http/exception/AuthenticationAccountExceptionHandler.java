package com.jeepclub.backend.authentication.api.http.exception;

import com.jeepclub.backend.authentication.core.application.exceptions.account.AuthenticationAccountAccessDeniedException;
import com.jeepclub.backend.authentication.core.application.exceptions.account.AuthenticationAccountNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.login.InvalidCredentialsException;
import com.jeepclub.backend.authentication.core.application.exceptions.login.PasswordChangeNotRequiredException;
import com.jeepclub.backend.authentication.core.domain.exception.account.AuthenticationAccountAlreadyDisabledException;
import com.jeepclub.backend.authentication.core.domain.exception.account.AuthenticationAccountBlockedException;
import com.jeepclub.backend.authentication.core.domain.exception.account.AuthenticationAccountCannotChangePasswordException;
import com.jeepclub.backend.authentication.core.domain.exception.account.AuthenticationAccountHashRequiredException;
import com.jeepclub.backend.authentication.core.domain.exception.account.AuthenticationAccountNotDisabledException;
import com.jeepclub.backend.authentication.core.domain.exception.account.AuthenticationAccountNotLockedException;
import com.jeepclub.backend.authentication.core.domain.exception.account.AuthenticationAccountPasswordChangeRequiredException;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.authentication")
public class AuthenticationAccountExceptionHandler extends ApiExceptionHandler {

    @ExceptionHandler(AuthenticationAccountBlockedException.class)
    public ResponseEntity<ApiErrorResponse> handleBlocked(AuthenticationAccountBlockedException exception) {
        return buildErrorResponse("USER_BLOCKED_FOR_LOGIN", exception.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationAccountAccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AuthenticationAccountAccessDeniedException exception) {
        return buildErrorResponse("USER_DISABLED", exception.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationAccountCannotChangePasswordException.class)
    public ResponseEntity<ApiErrorResponse> handleCannotChangePassword(
            AuthenticationAccountCannotChangePasswordException exception
    ) {
        return buildErrorResponse("USER_CANNOT_CHANGE_PASSWORD", exception.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationAccountHashRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handleHashRequired(AuthenticationAccountHashRequiredException exception) {
        return buildErrorResponse("USER_NEW_HASH_REQUIRED", exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AuthenticationAccountNotLockedException.class)
    public ResponseEntity<ApiErrorResponse> handleNotLocked(AuthenticationAccountNotLockedException exception) {
        return buildErrorResponse("USER_NOT_LOCKOUT", exception.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AuthenticationAccountPasswordChangeRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handlePasswordChangeRequired(
            AuthenticationAccountPasswordChangeRequiredException exception
    ) {
        return buildErrorResponse("USER_PASSWORD_CHANGE_REQUIRED", exception.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AuthenticationAccountNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(AuthenticationAccountNotFoundException exception) {
        return buildErrorResponse("AUTHENTICATION_ACCOUNT_NOT_FOUND", exception.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AuthenticationAccountAlreadyDisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleAlreadyDisabled(
            AuthenticationAccountAlreadyDisabledException exception
    ) {
        return buildErrorResponse("USER_ALREADY_DISABLED", exception.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AuthenticationAccountNotDisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleNotDisabled(AuthenticationAccountNotDisabledException exception) {
        return buildErrorResponse("USER_NOT_DISABLED", exception.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(InvalidCredentialsException exception) {
        return buildErrorResponse("INVALID_CREDENTIALS", exception.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(PasswordChangeNotRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handlePasswordChangeNotRequired(
            PasswordChangeNotRequiredException exception
    ) {
        return buildErrorResponse("USER_PASSWORD_CHANGE_NOT_REQUIRED", exception.getMessage(), HttpStatus.CONFLICT);
    }
}
