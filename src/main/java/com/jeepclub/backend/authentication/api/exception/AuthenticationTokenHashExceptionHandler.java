package com.jeepclub.backend.authentication.api.exception;

import com.jeepclub.backend.authentication.core.application.exceptions.tokenhash.TokenInvalidException;
import com.jeepclub.backend.authentication.core.application.exceptions.tokenhash.TokenNotFoundException;
import com.jeepclub.backend.infra.web.exception.ApiErrorResponse;
import com.jeepclub.backend.infra.web.exception.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.authentication")
public class AuthenticationTokenHashExceptionHandler extends ApiExceptionHandler {

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
}