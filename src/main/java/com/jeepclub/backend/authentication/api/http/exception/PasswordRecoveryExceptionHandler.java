package com.jeepclub.backend.authentication.api.http.exception;

import com.jeepclub.backend.authentication.core.application.exceptions.login.PasswordRecoveryRequestNotFoundException;
import com.jeepclub.backend.authentication.core.domain.exception.passwordrecovery.PasswordRecoveryRequestStateException;
import com.jeepclub.backend.authentication.core.domain.exception.passwordrecovery.PasswordRecoveryRequestValidationException;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(
        basePackages = "com.jeepclub.backend.authentication"
)
public class PasswordRecoveryExceptionHandler
        extends ApiExceptionHandler {

    @ExceptionHandler(
            PasswordRecoveryRequestValidationException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handlePasswordRecoveryRequestValidation(
            PasswordRecoveryRequestValidationException exception
    ) {
        return buildErrorResponse(
                "PASSWORD_RECOVERY_REQUEST_INVALID",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(
            PasswordRecoveryRequestStateException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handlePasswordRecoveryRequestState(
            PasswordRecoveryRequestStateException exception
    ) {
        return buildErrorResponse(
                "PASSWORD_RECOVERY_REQUEST_STATE_CONFLICT",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(
            PasswordRecoveryRequestNotFoundException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handlePasswordRecoveryRequestNotFound(
            PasswordRecoveryRequestNotFoundException exception
    ) {
        return buildErrorResponse(
                "PASSWORD_RECOVERY_REQUEST_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }
}