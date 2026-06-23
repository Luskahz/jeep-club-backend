package com.jeepclub.backend.authentication.api.http.exception;

import com.jeepclub.backend.authentication.core.application.exceptions.login.PasswordChangeChallengeInvalidException;
import com.jeepclub.backend.authentication.core.domain.exception.passwordchangechallenge.PasswordChangeChallengeStateException;
import com.jeepclub.backend.authentication.core.domain.exception.passwordchangechallenge.PasswordChangeChallengeValidationException;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(
        basePackages = "com.jeepclub.backend.authentication"
)
public class PasswordChangeChallengeExceptionHandler
        extends ApiExceptionHandler {

    @ExceptionHandler(
            PasswordChangeChallengeValidationException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handlePasswordChangeChallengeValidation(
            PasswordChangeChallengeValidationException exception
    ) {
        return buildErrorResponse(
                "PASSWORD_CHANGE_CHALLENGE_INVALID_DATA",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(
            PasswordChangeChallengeStateException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handlePasswordChangeChallengeState(
            PasswordChangeChallengeStateException exception
    ) {
        return buildErrorResponse(
                "PASSWORD_CHANGE_CHALLENGE_STATE_CONFLICT",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(
            PasswordChangeChallengeInvalidException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handlePasswordChangeChallengeInvalid(
            PasswordChangeChallengeInvalidException exception
    ) {
        return buildErrorResponse(
                "PASSWORD_CHANGE_CHALLENGE_INVALID",
                exception.getMessage(),
                HttpStatus.UNAUTHORIZED
        );
    }
}