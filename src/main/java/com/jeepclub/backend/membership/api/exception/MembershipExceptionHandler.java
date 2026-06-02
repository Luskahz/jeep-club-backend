package com.jeepclub.backend.membership.api.exception;

import com.jeepclub.backend.infra.web.exception.ApiErrorResponse;
import com.jeepclub.backend.membership.core.application.exception.MemberActivationTokenNotFoundException;
import com.jeepclub.backend.membership.core.application.exception.MembershipApplicationAlreadyExistsException;
import com.jeepclub.backend.membership.core.application.exception.MembershipApplicationAlreadyProcessedException;
import com.jeepclub.backend.membership.core.application.exception.MembershipApplicationNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.membership.api")
public class MembershipExceptionHandler {

    @ExceptionHandler(MembershipApplicationNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(MembershipApplicationNotFoundException ex) {
        return build("MEMBERSHIP_APPLICATION_NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MembershipApplicationAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleAlreadyExists(MembershipApplicationAlreadyExistsException ex) {
        return build("MEMBERSHIP_APPLICATION_ALREADY_EXISTS", ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MembershipApplicationAlreadyProcessedException.class)
    public ResponseEntity<ApiErrorResponse> handleAlreadyProcessed(MembershipApplicationAlreadyProcessedException ex) {
        return build("MEMBERSHIP_APPLICATION_ALREADY_PROCESSED", ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MemberActivationTokenNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleTokenNotFound(MemberActivationTokenNotFoundException ex) {
        return build("ACTIVATION_TOKEN_NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(IllegalStateException ex) {
        return build("INVALID_OPERATION", ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private ResponseEntity<ApiErrorResponse> build(String code, String message, HttpStatus status) {
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(code, message, status));
    }
}