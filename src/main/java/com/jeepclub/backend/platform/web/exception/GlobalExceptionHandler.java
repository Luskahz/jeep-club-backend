package com.jeepclub.backend.platform.web.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception
    ) {
        List<ValidationFieldErrorResponse> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(GlobalExceptionHandler::toFieldErrorResponse)
                .toList();

        ValidationErrorResponse response = new ValidationErrorResponse(
                Instant.now(),
                "VALIDATION_ERROR",
                "Erro de validação nos campos enviados.",
                HttpStatus.BAD_REQUEST.value(),
                fieldErrors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    private static ValidationFieldErrorResponse toFieldErrorResponse(FieldError fieldError) {
        return new ValidationFieldErrorResponse(
                fieldError.getField(),
                fieldError.getDefaultMessage(),
                fieldError.getRejectedValue()
        );
    }
}