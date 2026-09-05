package com.jeepclub.backend.iam.authentication.api.http.exception;

import com.jeepclub.backend.iam.authentication.core.application.exceptions.refreshtoken.RefreshTokenInvalidException;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.refreshtoken.RefreshTokenNotFoundException;
import com.jeepclub.backend.iam.authentication.core.domain.exception.refreshtoken.RefreshTokenStateException;
import com.jeepclub.backend.iam.authentication.core.domain.exception.refreshtoken.RefreshTokenValidationException;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(
        basePackages = "com.jeepclub.backend.iam.authentication"
)
public class RefreshTokenExceptionHandler
        extends ApiExceptionHandler {

    @ExceptionHandler(
            RefreshTokenValidationException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleRefreshTokenValidation(
            RefreshTokenValidationException exception
    ) {
        return buildErrorResponse(
                "REFRESH_TOKEN_INVALID_DATA",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(
            RefreshTokenStateException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleRefreshTokenState(
            RefreshTokenStateException exception
    ) {
        return buildErrorResponse(
                "REFRESH_TOKEN_STATE_CONFLICT",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(
            RefreshTokenInvalidException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleRefreshTokenInvalid(
            RefreshTokenInvalidException exception
    ) {
        return buildErrorResponse(
                "REFRESH_TOKEN_INVALID",
                exception.getMessage(),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(
            RefreshTokenNotFoundException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleRefreshTokenNotFound(
            RefreshTokenNotFoundException exception
    ) {
        return buildErrorResponse(
                "REFRESH_TOKEN_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }
}
