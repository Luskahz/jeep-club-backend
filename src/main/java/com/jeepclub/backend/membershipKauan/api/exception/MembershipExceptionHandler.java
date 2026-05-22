package com.jeepclub.backend.membershipKauan.api.exception;

import com.jeepclub.backend.infra.web.exception.ApiErrorResponse;
import com.jeepclub.backend.membershipKauan.core.domain.exception.DependentException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.membershipKauan.api")
public class MembershipExceptionHandler {

    @ExceptionHandler(DependentException.class)
    public ResponseEntity<ApiErrorResponse> handleDependentException(DependentException exception) {
        String message = exception.getMessage();
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String code = "DEPENDENT_BUSINESS_RULE_VIOLATION";

        if (message.contains("não encontrado")) {
            status = HttpStatus.NOT_FOUND;
            code = "DEPENDENT_NOT_FOUND";
        } else if (message.contains("permissão")) {
            status = HttpStatus.FORBIDDEN;
            code = "DEPENDENT_ACCESS_DENIED";
        } else if (message.contains("Já existe")) {
            status = HttpStatus.CONFLICT;
            code = "DEPENDENT_CONFLICT";
        }

        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(code, message, status));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(IllegalArgumentException exception) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of("INVALID_ARGUMENT", exception.getMessage(), status));
    }
}
