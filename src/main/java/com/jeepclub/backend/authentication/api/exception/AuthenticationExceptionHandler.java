package com.jeepclub.backend.authentication.api.exception;

import com.jeepclub.backend.authentication.core.domain.exception.*;
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




}