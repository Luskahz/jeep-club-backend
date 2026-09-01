package com.jeepclub.backend.health.api.http.exception;

import com.jeepclub.backend.health.core.application.exceptions.DependentOwnershipValidationUnavailableException;
import com.jeepclub.backend.health.core.application.exceptions.InvalidMedicalProfileDataException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileAccessDeniedException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileNotFoundException;
import com.jeepclub.backend.health.core.domain.exception.InvalidMedicalProfileException;
import com.jeepclub.backend.health.core.domain.exception.MedicalProfileAlreadyDeletedException;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.health")
public class MedicalProfileExceptionHandler extends ApiExceptionHandler {

    @ExceptionHandler(MedicalProfileNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleMedicalProfileNotFound(
            MedicalProfileNotFoundException exception
    ) {
        return buildErrorResponse(
                "MEDICAL_PROFILE_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler({
            InvalidMedicalProfileDataException.class,
            InvalidMedicalProfileException.class
    })
    public ResponseEntity<ApiErrorResponse> handleInvalidMedicalProfileData(
            RuntimeException exception
    ) {
        return buildErrorResponse(
                "MEDICAL_PROFILE_INVALID_DATA",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(MedicalProfileAccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleMedicalProfileAccessDenied(
            MedicalProfileAccessDeniedException exception
    ) {
        return buildErrorResponse(
                "MEDICAL_PROFILE_ACCESS_DENIED",
                exception.getMessage(),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(DependentOwnershipValidationUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleDependentOwnershipValidationUnavailable(
            DependentOwnershipValidationUnavailableException exception
    ) {
        return buildErrorResponse(
                "DEPENDENT_OWNERSHIP_VALIDATION_UNAVAILABLE",
                exception.getMessage(),
                HttpStatus.SERVICE_UNAVAILABLE
        );
    }

    @ExceptionHandler(MedicalProfileAlreadyDeletedException.class)
    public ResponseEntity<ApiErrorResponse> handleMedicalProfileAlreadyDeleted(
            MedicalProfileAlreadyDeletedException exception
    ) {
        return buildErrorResponse(
                "MEDICAL_PROFILE_ALREADY_DELETED",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }
}
