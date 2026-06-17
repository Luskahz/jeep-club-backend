package com.jeepclub.backend.authentication.api.exception;

import com.jeepclub.backend.authentication.core.application.exceptions.refreshtoken.RefreshTokenInvalidException;
import com.jeepclub.backend.authentication.core.application.exceptions.refreshtoken.RefreshTokenNotFoundException;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.authentication")
public class RefreshTokenExceptionHandler extends ApiExceptionHandler {

    @ExceptionHandler(RefreshTokenInvalidException.class)
    public ResponseEntity<ApiErrorResponse> handleRefreshTokenInvalid(RefreshTokenInvalidException exception) {
        return buildErrorResponse(
                "REFRESH_TOKEN_INVALID",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(RefreshTokenNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleRefreshTokenNotFound(
            RefreshTokenNotFoundException exception
    ) {
        return buildErrorResponse(
                "REFRESH_TOKEN_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }
}