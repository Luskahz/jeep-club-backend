package com.jeepclub.backend.authentication.api.exception;

import com.jeepclub.backend.authentication.core.application.exceptions.refreshtoken.RFInvalidException;
import com.jeepclub.backend.authentication.core.application.exceptions.refreshtoken.RFNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.session.SessionInvalidException;
import com.jeepclub.backend.authentication.core.application.exceptions.session.SessionNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.session.SessionUserMismatchException;
import com.jeepclub.backend.authentication.core.application.exceptions.tokenhash.TokenInvalidException;
import com.jeepclub.backend.authentication.core.application.exceptions.tokenhash.TokenNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.*;
import com.jeepclub.backend.authentication.core.domain.exception.session.*;
import com.jeepclub.backend.authentication.core.domain.exception.user.*;
import com.jeepclub.backend.infra.web.exception.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public  class AuthenticationExceptionHandler {


    ///////////////////////////
    ///                     ///
    /// User                ///
    ///                     ///
    ///////////////////////////

    //domain

    @ExceptionHandler(UserCpfNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleCpfNotFound(UserCpfNotFoundException exception) {
        return buildErrorResponse(
                "USER_CPF_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }


    @ExceptionHandler(UserInvalidPasswordException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidPassword(UserInvalidPasswordException exception) {
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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return buildErrorResponse(
                "ILLEGAL_ARGUMENT",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
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
    public ResponseEntity<ApiErrorResponse> handleUserPasswordChangeRequired(
            UserPasswordChangeRequiredException exception
    ) {
        return buildErrorResponse(
                "USER_PASSWORD_CHANGE_REQUIRED",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }


    // services

    @ExceptionHandler(UserCpfInvalidException.class)
    public ResponseEntity<ApiErrorResponse> handleUserCpfInvalid(UserCpfInvalidException exception) {
        return buildErrorResponse(
                "USER_CPF_INVALID",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(UserCpfNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserCpfNotFound(UserCpfNotFoundException exception) {
        return buildErrorResponse(
                "USER_CPF_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
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

    @ExceptionHandler(UserInvalidPasswordException.class)
    public ResponseEntity<ApiErrorResponse> handleUserInvalidPassword(UserInvalidPasswordException exception) {
        return buildErrorResponse(
                "USER_INVALID_PASSWORD",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }



    ///////////////////////////
    ///                     ///
    /// Session             ///
    ///                     ///
    ///////////////////////////


    //domain

    @ExceptionHandler(SessionInvalidActiveStateException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionInvalidActiveState(SessionInvalidActiveStateException exception) {
        return buildErrorResponse(
                "SESSION_INVALID_ACTIVE_STATE",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(SessionInvalidExpirationDateException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionInvalidExpirationDate(SessionInvalidExpirationDateException exception) {
        return buildErrorResponse(
                "SESSION_INVALID_EXPIRATION_DATE",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(SessionInvalidLogoutStateException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionInvalidLogoutState(SessionInvalidLogoutStateException exception) {
        return buildErrorResponse(
                "SESSION_INVALID_LOGOUT_STATE",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(SessionInvalidRevokeStateException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionInvalidRevokeState(SessionInvalidRevokeStateException exception) {
        return buildErrorResponse(
                "SESSION_INVALID_REVOKE_STATE",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(SessionInvalidTtlValueException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionInvalidTtlValue(SessionInvalidTtlValueException exception) {
        return buildErrorResponse(
                "SESSION_INVALID_TTL_VALUE",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(SessionMissingCreatedAtException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionMissingCreatedAt(SessionMissingCreatedAtException exception) {
        return buildErrorResponse(
                "SESSION_MISSING_CREATED_AT",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(SessionMissingExpiresAtException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionMissingExpiresAt(SessionMissingExpiresAtException exception) {
        return buildErrorResponse(
                "SESSION_MISSING_EXPIRES_AT",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(SessionMissingIdException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionMissingId(SessionMissingIdException exception) {
        return buildErrorResponse(
                "SESSION_MISSING_ID",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(SessionMissingStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionMissingStatus(SessionMissingStatusException exception) {
        return buildErrorResponse(
                "SESSION_MISSING_STATUS",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(SessionMissingTtlException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionMissingTtl(SessionMissingTtlException exception) {
        return buildErrorResponse(
                "SESSION_MISSING_TTL",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(SessionMissingUserIdException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionMissingUserId(SessionMissingUserIdException exception) {
        return buildErrorResponse(
                "SESSION_MISSING_USER_ID",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(SessionNotActiveException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionNotActive(SessionNotActiveException exception) {
        return buildErrorResponse(
                "SESSION_NOT_ACTIVE",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(SessionNowInstantRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionNowInstantRequired(SessionNowInstantRequiredException exception) {
        return buildErrorResponse(
                "SESSION_NOW_INSTANT_REQUIRED",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    //service

    @ExceptionHandler(SessionInvalidException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionInvalid(SessionInvalidException exception) {
        return buildErrorResponse(
                "SESSION_INVALID",
                exception.getMessage(),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionNotFound(SessionNotFoundException exception) {
        return buildErrorResponse(
                "SESSION_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(SessionUserMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionUserMismatch(SessionUserMismatchException exception) {
        return buildErrorResponse(
                "SESSION_USER_MISMATCH",
                exception.getMessage(),
                HttpStatus.FORBIDDEN
        );
    }

    ///////////////////////////
    ///                     ///
    /// TOKENHASH           ///
    ///                     ///
    ///////////////////////////

    //service

    @ExceptionHandler(TokenInvalidException.class)
    public ResponseEntity<ApiErrorResponse> handleTokenInvalid(TokenInvalidException exception) {
        return buildErrorResponse(
                "TOKEN_INVALID",
                exception.getMessage(),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(TokenNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleTokenNotFound(TokenNotFoundException exception) {
        return buildErrorResponse(
                "TOKEN_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    ///////////////////////////
    ///                     ///
    /// REFFRESHTOkEN       ///
    ///                     ///
    ///////////////////////////

    //service

    @ExceptionHandler(RFInvalidException.class)
    public ResponseEntity<ApiErrorResponse> handleRFInvalid(RFInvalidException exception) {
        return buildErrorResponse(
                "RF_INVALID",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(RFNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleRFNotFound(RFNotFoundException exception) {
        return buildErrorResponse(
                "RF_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }


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