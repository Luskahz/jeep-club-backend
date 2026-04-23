package com.jeepclub.backend.authentication.api.exception;

import com.jeepclub.backend.authentication.core.domain.exception.CpfNotFoundException;
import com.jeepclub.backend.authentication.core.domain.exception.InvalidPasswordException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private Map<String, Object> resposta(String msg, HttpStatus status) {
        Map<String, Object> erro = new HashMap<>();
        erro.put("mensagem", msg);
        erro.put("status", status.value());
        erro.put("timestamp", LocalDateTime.now());
        return erro;
    }

    @ExceptionHandler(CpfNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCpfNotFound(CpfNotFoundException ex) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(resposta(ex.getMessage(), status));
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidPassword(InvalidPasswordException ex) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status).body(resposta(ex.getMessage(), status));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(resposta(ex.getMessage(), status));
    }
}