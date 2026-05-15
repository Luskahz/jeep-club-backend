package com.jeepclub.backend.authentication.api.exception;

import com.jeepclub.backend.authentication.core.application.exceptions.user.UserCpfNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserInvalidPasswordException;
import com.jeepclub.backend.authentication.core.domain.exception.session.*;
import com.jeepclub.backend.authentication.core.domain.exception.user.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public  class AuthenticationExceptionHandler {

    private Map<String, Object> resposta(String msg, HttpStatus status) {
        Map<String, Object> erro = new HashMap<>();
        erro.put("mensagem", msg);
        erro.put("status", status.value());
        erro.put("timestamp", LocalDateTime.now());
        return erro;
    }


    ///////////////////////////
    ///                     ///
    /// User                ///
    ///                     ///
    ///////////////////////////
    @ExceptionHandler(UserCpfNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCpfNotFound( UserCpfNotFoundException ex) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(resposta(ex.getMessage(), status));
    }

    @ExceptionHandler(UserInvalidPasswordException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidPassword(UserInvalidPasswordException ex) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status).body(resposta(ex.getMessage(), status));
    }

    @ExceptionHandler(UserBlockedForLoginException.class)
    public ResponseEntity<Map<String, Object>> handleUserBlockedForLogin(UserBlockedForLoginException ex) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        return ResponseEntity.status(status).body(resposta(ex.getMessage(), status));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(resposta(ex.getMessage(), status));
    }

    @ExceptionHandler(UserCannotChangePasswordException.class)
    public ResponseEntity<Map<String, Object>> handleUserCannotChangePassword(UserCannotChangePasswordException ex) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        return ResponseEntity.status(status).body(resposta(ex.getMessage(), status));
    }

    @ExceptionHandler(UserIdRequiredException.class)
    public ResponseEntity<Map<String, Object>> handleUserIdRequired(UserIdRequiredException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(resposta(ex.getMessage(), status));
    }

    @ExceptionHandler(UserLockoutException.class)
    public ResponseEntity<Map<String, Object>> handleUserLockout(UserLockoutException ex) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        return ResponseEntity.status(status).body(resposta(ex.getMessage(), status));
    }

    @ExceptionHandler(UserNewHashRequiredException.class)
    public ResponseEntity<Map<String, Object>> handleUserNewHashRequired(UserNewHashRequiredException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(resposta(ex.getMessage(), status));
    }

    @ExceptionHandler(UserNotDisableException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotDisable(UserNotDisableException ex) {
        HttpStatus status = HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(resposta(ex.getMessage(), status));
    }

    @ExceptionHandler(UserNotLockoutException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotLockout(UserNotLockoutException ex) {
        HttpStatus status = HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(resposta(ex.getMessage(), status));
    }

    @ExceptionHandler(UserNowInstantRequiredException.class)
    public ResponseEntity<Map<String, Object>> handleUserNowInstantRequired(UserNowInstantRequiredException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(resposta(ex.getMessage(), status));
    }

    ///////////////////////////
    ///                     ///
    /// Session             ///
    ///                     ///
    ///////////////////////////
    @ExceptionHandler(SessionInvalidActiveStateException.class)
    public ResponseEntity<Map<String, Object>> handleSessionInvalidActiveState(SessionInvalidActiveStateException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(resposta(ex.getMessage(), status));
    }

    @ExceptionHandler(SessionInvalidExpirationDateException.class)
    public ResponseEntity<Map<String, Object>> handleSessionInvalidExpirationDate(SessionInvalidExpirationDateException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(resposta(ex.getMessage(), status));
    }

    @ExceptionHandler(SessionInvalidLogoutStateException.class)
    public ResponseEntity<Map<String, Object>> handleSessionInvalidLogoutState(SessionInvalidLogoutStateException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(resposta(ex.getMessage(), status));
    }

    @ExceptionHandler(SessionInvalidRevokeStateException.class)
    public ResponseEntity<Map<String, Object>> handleSessionInvalidRevokeState(SessionInvalidRevokeStateException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(resposta(ex.getMessage(), status));
    }

    @ExceptionHandler(SessionInvalidTtlValueException.class)
    public ResponseEntity<Map<String, Object>> handleSessionInvalidTtlValue(SessionInvalidTtlValueException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(resposta(ex.getMessage(), status));
    }

    @ExceptionHandler(SessionMissingCreatedAtException.class)
    public ResponseEntity<Map<String, Object>> handleSessionMissingCreatedAt(SessionMissingCreatedAtException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(resposta(ex.getMessage(), status));
    }

    @ExceptionHandler(SessionMissingExpiresAtException.class)
    public ResponseEntity<Map<String, Object>> handleSessionMissingExpiresAt(SessionMissingExpiresAtException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(resposta(ex.getMessage(), status));
    }

    @ExceptionHandler(SessionMissingIdException.class)
    public ResponseEntity<Map<String, Object>> handleSessionMissingId(SessionMissingIdException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(resposta(ex.getMessage(), status));
    }

    @ExceptionHandler(SessionMissingStatusException.class)
    public ResponseEntity<Map<String, Object>> handleSessionMissingStatus(SessionMissingStatusException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(resposta(ex.getMessage(), status));
    }

    @ExceptionHandler(SessionMissingTtlException.class)
    public ResponseEntity<Map<String, Object>> handleSessionMissingTtl(SessionMissingTtlException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(resposta(ex.getMessage(), status));
    }

    @ExceptionHandler(SessionMissingUserIdException.class)
    public ResponseEntity<Map<String, Object>> handleSessionMissingUserId(SessionMissingUserIdException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(resposta(ex.getMessage(), status));
    }

    @ExceptionHandler(SessionNotActiveException.class)
    public ResponseEntity<Map<String, Object>> handleSessionNotActive(SessionNotActiveException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(resposta(ex.getMessage(), status));
    }

    @ExceptionHandler(SessionNowInstantRequiredException.class)
    public ResponseEntity<Map<String, Object>> handleSessionNowInstantRequired(SessionNowInstantRequiredException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(resposta(ex.getMessage(), status));
    }





}