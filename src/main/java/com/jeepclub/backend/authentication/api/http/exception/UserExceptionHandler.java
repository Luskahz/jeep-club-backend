package com.jeepclub.backend.authentication.api.http.exception;

import com.jeepclub.backend.authentication.core.application.exceptions.login.InvalidCredentialsException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.RegistrationConflictException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserCpfInvalidException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserDisabledException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserIdNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserCpfNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserInvalidCredentialsException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserInvalidPasswordException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserPasswordChangeNotRequiredException;
import com.jeepclub.backend.authentication.core.domain.exception.user.UserAlreadyDisabledException;
import com.jeepclub.backend.authentication.core.domain.exception.user.UserBlockedForLoginException;
import com.jeepclub.backend.authentication.core.domain.exception.user.UserCannotChangePasswordException;
import com.jeepclub.backend.authentication.core.domain.exception.user.UserIdRequiredException;
import com.jeepclub.backend.authentication.core.domain.exception.user.UserLockoutException;
import com.jeepclub.backend.authentication.core.domain.exception.user.UserNewHashRequiredException;
import com.jeepclub.backend.authentication.core.domain.exception.user.UserNotDisabledException;
import com.jeepclub.backend.authentication.core.domain.exception.user.UserNotLockoutException;
import com.jeepclub.backend.authentication.core.domain.exception.user.UserNowInstantRequiredException;
import com.jeepclub.backend.authentication.core.domain.exception.user.UserPasswordChangeRequiredException;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.authentication")
public class UserExceptionHandler extends ApiExceptionHandler {

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

    @ExceptionHandler(UserAlreadyDisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleUserAlreadyDisabled(
            UserAlreadyDisabledException exception
    ) {
        return buildErrorResponse(
                "USER_ALREADY_DISABLED",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(UserNotDisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotDisabled(
            UserNotDisabledException exception
    ) {
        return buildErrorResponse(
                "USER_NOT_DISABLED",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException exception
    ) {
        return buildErrorResponse(
                "INVALID_CREDENTIALS",
                exception.getMessage(),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(RegistrationConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleRegistrationConflict(
            RegistrationConflictException exception
    ) {
        return buildErrorResponse(
                "REGISTRATION_CONFLICT",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(UserCpfNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserCpfNotFound(UserCpfNotFoundException exception) {
        return buildErrorResponse("USER_CPF_NOT_FOUND", exception.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({
            UserInvalidCredentialsException.class,
            UserInvalidPasswordException.class
    })
    public ResponseEntity<ApiErrorResponse> handleUserInvalidCredentials(RuntimeException exception) {
        return buildErrorResponse("INVALID_CREDENTIALS", exception.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UserPasswordChangeNotRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handleUserPasswordChangeNotRequired(
            UserPasswordChangeNotRequiredException exception
    ) {
        return buildErrorResponse(
                "USER_PASSWORD_CHANGE_NOT_REQUIRED",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }
}
