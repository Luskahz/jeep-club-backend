package com.jeepclub.backend.authentication.api.exception;

import com.jeepclub.backend.authentication.core.application.exceptions.refreshtoken.RFInvalidException;
import com.jeepclub.backend.authentication.core.application.exceptions.refreshtoken.RFNotFoundException;
import com.jeepclub.backend.infra.web.exception.ApiErrorResponse;
import com.jeepclub.backend.infra.web.exception.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.authentication")
public class AuthenticationRefreshTokenExceptionHandler extends ApiExceptionHandler {

    @ExceptionHandler(RFInvalidException.class)
    public ResponseEntity<ApiErrorResponse> handleRFInvalid(RFInvalidException exception) {
        return buildErrorResponse(
                "RF_INVALID",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(RFNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleRFNotFound(RFNotFoundException exception) {
        return buildErrorResponse(
                "RF_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }
}