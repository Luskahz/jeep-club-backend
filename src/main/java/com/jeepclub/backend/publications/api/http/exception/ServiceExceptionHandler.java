package com.jeepclub.backend.publications.api.http.exception;

import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import com.jeepclub.backend.platform.web.exception.ValidationFieldErrorResponse;
import com.jeepclub.backend.publications.api.http.controller.admin.AdminServicePublicationChangeRequestController;
import com.jeepclub.backend.publications.api.http.controller.admin.AdminServicePublicationController;
import com.jeepclub.backend.publications.api.http.controller.admin.AdminServicePublicationRequestController;
import com.jeepclub.backend.publications.api.http.controller.member.ServicePublicationChangeRequestController;
import com.jeepclub.backend.publications.api.http.controller.member.ServicePublicationController;
import com.jeepclub.backend.publications.api.http.controller.member.ServicePublicationRequestController;
import com.jeepclub.backend.publications.api.http.dto.MalformedServicePayloadException;
import com.jeepclub.backend.publications.core.application.exception.ServiceOperationException;
import com.jeepclub.backend.publications.core.domain.exception.PublicationAlreadyDeletedException;
import com.jeepclub.backend.shared.storage.exception.InvalidStorageKeyException;
import com.jeepclub.backend.shared.storage.exception.StorageObjectNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Locale;

@RestControllerAdvice(assignableTypes = {
        ServicePublicationRequestController.class, ServicePublicationController.class,
        ServicePublicationChangeRequestController.class, AdminServicePublicationRequestController.class,
        AdminServicePublicationChangeRequestController.class, AdminServicePublicationController.class
})
public class ServiceExceptionHandler extends ApiExceptionHandler {
    @ExceptionHandler(ServiceOperationException.class)
    public ResponseEntity<ApiErrorResponse> operation(ServiceOperationException exception) {
        return switch (exception.reason()) {
            case REQUEST_NOT_FOUND -> error("SERVICE_REQUEST_NOT_FOUND", HttpStatus.NOT_FOUND);
            case REQUEST_ALREADY_PROCESSED -> error("SERVICE_REQUEST_ALREADY_PROCESSED", HttpStatus.CONFLICT);
            case CHANGE_REQUEST_NOT_FOUND -> error("SERVICE_CHANGE_REQUEST_NOT_FOUND", HttpStatus.NOT_FOUND);
            case CHANGE_REQUEST_ALREADY_PROCESSED -> error("SERVICE_CHANGE_REQUEST_ALREADY_PROCESSED", HttpStatus.CONFLICT);
            case CHANGE_REQUEST_ALREADY_PENDING -> error("SERVICE_CHANGE_REQUEST_ALREADY_PENDING", HttpStatus.CONFLICT);
            case SERVICE_NOT_FOUND -> error("SERVICE_NOT_FOUND", HttpStatus.NOT_FOUND);
            case SERVICE_NOT_OWNER -> error("SERVICE_NOT_OWNER", HttpStatus.FORBIDDEN);
            case SERVICE_INVALID_STATE -> error("SERVICE_INVALID_STATE", HttpStatus.CONFLICT);
            case INVALID_REQUEST -> error("SERVICE_INVALID_REQUEST", HttpStatus.BAD_REQUEST);
        };
    }

    @ExceptionHandler({MalformedServicePayloadException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiErrorResponse> invalid(RuntimeException exception) {
        return error("SERVICE_INVALID_REQUEST", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> validation(ConstraintViolationException exception) {
        var response = buildErrorResponse("VALIDATION_ERROR", "Service fields are invalid.", HttpStatus.BAD_REQUEST);
        response.getBody().setErrors(exception.getConstraintViolations().stream()
                .map(ServiceExceptionHandler::fieldError).toList());
        return response;
    }

    @ExceptionHandler(InvalidStorageKeyException.class)
    public ResponseEntity<ApiErrorResponse> invalidImage(InvalidStorageKeyException exception) {
        return error("INVALID_IMAGE", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(StorageObjectNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> missingImage(StorageObjectNotFoundException exception) {
        return error("IMAGE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({PublicationAlreadyDeletedException.class, EntityNotFoundException.class})
    public ResponseEntity<ApiErrorResponse> missingService(RuntimeException exception) {
        return error("SERVICE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({DataIntegrityViolationException.class, ConcurrencyFailureException.class,
            OptimisticLockException.class, PessimisticLockException.class, LockTimeoutException.class})
    public ResponseEntity<ApiErrorResponse> conflict(RuntimeException exception) {
        return error("SERVICE_CONFLICT", HttpStatus.CONFLICT);
    }

    private ResponseEntity<ApiErrorResponse> error(String code, HttpStatus status) {
        return buildErrorResponse(code, "Service operation could not be completed.", status);
    }

    private static ValidationFieldErrorResponse fieldError(ConstraintViolation<?> violation) {
        String code = violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName()
                .replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
        return new ValidationFieldErrorResponse(violation.getPropertyPath().toString(), code, violation.getMessage());
    }
}
