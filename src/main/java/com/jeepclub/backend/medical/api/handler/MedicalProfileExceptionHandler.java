package com.jeepclub.backend.medical.api.handler;

import com.jeepclub.backend.medical.api.dto.ApiErrorResponse;
import com.jeepclub.backend.medical.core.application.exceptions.DependentOwnershipValidationUnavailableException;
import com.jeepclub.backend.medical.core.application.exceptions.InvalidMedicalProfileDataException;
import com.jeepclub.backend.medical.core.application.exceptions.MedicalProfileAccessDeniedException;
import com.jeepclub.backend.medical.core.application.exceptions.MedicalProfileNotFoundException;
import com.jeepclub.backend.medical.core.domain.exceptions.InvalidMedicalProfileException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class MedicalProfileExceptionHandler {

    @ExceptionHandler(MedicalProfileNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            MedicalProfileNotFoundException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(
                        HttpStatus.NOT_FOUND.value(),
                        HttpStatus.NOT_FOUND.getReasonPhrase(),
                        exception.getMessage()
                ));
    }

    @ExceptionHandler({
            InvalidMedicalProfileDataException.class,
            InvalidMedicalProfileException.class
    })
    public ResponseEntity<ApiErrorResponse> handleInvalidData(
            RuntimeException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(MedicalProfileAccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            MedicalProfileAccessDeniedException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.of(
                        HttpStatus.FORBIDDEN.value(),
                        HttpStatus.FORBIDDEN.getReasonPhrase(),
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(DependentOwnershipValidationUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationUnavailable(
            DependentOwnershipValidationUnavailableException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_IMPLEMENTED)
                .body(ApiErrorResponse.of(
                        HttpStatus.NOT_IMPLEMENTED.value(),
                        HttpStatus.NOT_IMPLEMENTED.getReasonPhrase(),
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        List<String> details = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        "Erro de validação nos dados enviados.",
                        details
                ));
    }
}
