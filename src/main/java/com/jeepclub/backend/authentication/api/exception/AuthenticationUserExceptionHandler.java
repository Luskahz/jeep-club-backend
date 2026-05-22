package com.jeepclub.backend.authentication.api.exception;

import com.jeepclub.backend.authentication.core.application.exceptions.user.*;
import com.jeepclub.backend.authentication.core.domain.exception.user.*;
import com.jeepclub.backend.infra.web.exception.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.authentication")
public class AuthenticationUserExceptionHandler extends AuthenticationExceptionHandler {

    @ExceptionHandler(UserCpfNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserCpfNotFound(UserCpfNotFoundException exception) {
        return buildErrorResponse(
                "USER_CPF_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(UserInvalidPasswordException.class)
    public ResponseEntity<ApiErrorResponse> handleUserInvalidPassword(UserInvalidPasswordException exception) {
        return buildErrorResponse(
                "USER_INVALID_PASSWORD",
                exception.getMessage(),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(UserBlockedForLoginException.class)
    public ResponseEntity<ApiErrorResponse> handleUserBlockedForLogin(UserBlockedForLoginException exception) {
        return buildErrorResponse(
                "USER_BLOCKED_FOR_LOGIN",
                exception.getMessage(),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(UserCannotChangePasswordException.class)
    public ResponseEntity<ApiErrorResponse> handleUserCannotChangePassword(UserCannotChangePasswordException exception) {
        return buildErrorResponse(
                "USER_CANNOT_CHANGE_PASSWORD",
                exception.getMessage(),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(UserIdRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handleUserIdRequired(UserIdRequiredException exception) {
        return buildErrorResponse(
                "USER_ID_REQUIRED",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(UserLockoutException.class)
    public ResponseEntity<ApiErrorResponse> handleUserLockout(UserLockoutException exception) {
        return buildErrorResponse(
                "USER_LOCKOUT",
                exception.getMessage(),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(UserNewHashRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNewHashRequired(UserNewHashRequiredException exception) {
        return buildErrorResponse(
                "USER_NEW_HASH_REQUIRED",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(UserNotDisableException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotDisable(UserNotDisableException exception) {
        return buildErrorResponse(
                "USER_NOT_DISABLE",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(UserNotLockoutException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotLockout(UserNotLockoutException exception) {
        return buildErrorResponse(
                "USER_NOT_LOCKOUT",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(UserNowInstantRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNowInstantRequired(UserNowInstantRequiredException exception) {
        return buildErrorResponse(
                "USER_NOW_INSTANT_REQUIRED",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(UserPasswordChangeRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handleUserPasswordChangeRequired(UserPasswordChangeRequiredException exception) {
        return buildErrorResponse(
                "USER_PASSWORD_CHANGE_REQUIRED",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(UserCpfInvalidException.class)
    public ResponseEntity<ApiErrorResponse> handleUserCpfInvalid(UserCpfInvalidException exception) {
        return buildErrorResponse(
                "USER_CPF_INVALID",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(UserDisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleUserDisabled(UserDisabledException exception) {
        return buildErrorResponse(
                "USER_DISABLED",
                exception.getMessage(),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(UserIdNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserIdNotFound(UserIdNotFoundException exception) {
        return buildErrorResponse(
                "USER_ID_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }
}