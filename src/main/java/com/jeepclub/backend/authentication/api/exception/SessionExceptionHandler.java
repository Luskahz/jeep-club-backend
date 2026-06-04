package com.jeepclub.backend.authentication.api.exception;

import com.jeepclub.backend.authentication.core.application.exceptions.session.SessionInvalidException;
import com.jeepclub.backend.authentication.core.application.exceptions.session.SessionNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.session.SessionUserMismatchException;
import com.jeepclub.backend.authentication.core.domain.exception.session.*;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.authentication")
public class SessionExceptionHandler extends ApiExceptionHandler {

    @ExceptionHandler(SessionInvalidActiveStateException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionInvalidActiveState(SessionInvalidActiveStateException exception) {
        return buildErrorResponse("SESSION_INVALID_ACTIVE_STATE", exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SessionInvalidExpirationDateException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionInvalidExpirationDate(SessionInvalidExpirationDateException exception) {
        return buildErrorResponse("SESSION_INVALID_EXPIRATION_DATE", exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SessionInvalidLogoutStateException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionInvalidLogoutState(SessionInvalidLogoutStateException exception) {
        return buildErrorResponse("SESSION_INVALID_LOGOUT_STATE", exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SessionInvalidRevokeStateException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionInvalidRevokeState(SessionInvalidRevokeStateException exception) {
        return buildErrorResponse("SESSION_INVALID_REVOKE_STATE", exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SessionInvalidTtlValueException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionInvalidTtlValue(SessionInvalidTtlValueException exception) {
        return buildErrorResponse("SESSION_INVALID_TTL_VALUE", exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SessionMissingCreatedAtException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionMissingCreatedAt(SessionMissingCreatedAtException exception) {
        return buildErrorResponse("SESSION_MISSING_CREATED_AT", exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SessionMissingExpiresAtException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionMissingExpiresAt(SessionMissingExpiresAtException exception) {
        return buildErrorResponse("SESSION_MISSING_EXPIRES_AT", exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SessionMissingIdException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionMissingId(SessionMissingIdException exception) {
        return buildErrorResponse("SESSION_MISSING_ID", exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SessionMissingStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionMissingStatus(SessionMissingStatusException exception) {
        return buildErrorResponse("SESSION_MISSING_STATUS", exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SessionMissingTtlException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionMissingTtl(SessionMissingTtlException exception) {
        return buildErrorResponse("SESSION_MISSING_TTL", exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SessionMissingUserIdException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionMissingUserId(SessionMissingUserIdException exception) {
        return buildErrorResponse("SESSION_MISSING_USER_ID", exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SessionNotActiveException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionNotActive(SessionNotActiveException exception) {
        return buildErrorResponse("SESSION_NOT_ACTIVE", exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SessionNowInstantRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionNowInstantRequired(SessionNowInstantRequiredException exception) {
        return buildErrorResponse("SESSION_NOW_INSTANT_REQUIRED", exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SessionInvalidException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionInvalid(SessionInvalidException exception) {
        return buildErrorResponse("SESSION_INVALID", exception.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionNotFound(SessionNotFoundException exception) {
        return buildErrorResponse("SESSION_NOT_FOUND", exception.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(SessionUserMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionUserMismatch(SessionUserMismatchException exception) {
        return buildErrorResponse("SESSION_USER_MISMATCH", exception.getMessage(), HttpStatus.FORBIDDEN);
    }
}