package com.jeepclub.backend.publications.api.http.exception;

import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import com.jeepclub.backend.platform.web.exception.ValidationFieldErrorResponse;
import com.jeepclub.backend.publications.api.http.dto.MalformedNoticePayloadException;
import com.jeepclub.backend.publications.core.application.exception.InvalidNoticeStateException;
import com.jeepclub.backend.publications.core.application.exception.NoticeNotFoundException;
import com.jeepclub.backend.publications.core.domain.exception.PublicationAlreadyDeletedException;
import com.jeepclub.backend.shared.storage.exception.InvalidStorageKeyException;
import com.jeepclub.backend.shared.storage.exception.StorageObjectNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Locale;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.publications.api.http.controller")
public class NoticeExceptionHandler extends ApiExceptionHandler {
    @ExceptionHandler({NoticeNotFoundException.class, PublicationAlreadyDeletedException.class, EntityNotFoundException.class})
    public ResponseEntity<ApiErrorResponse> notFound(RuntimeException exception) {
        return buildErrorResponse("NOTICE_NOT_FOUND", "Notice was not found.", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidNoticeStateException.class)
    public ResponseEntity<ApiErrorResponse> invalidState(InvalidNoticeStateException exception) {
        return buildErrorResponse("NOTICE_INVALID_STATE", exception.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler({MalformedNoticePayloadException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiErrorResponse> invalidRequest(RuntimeException exception) {
        return buildErrorResponse("NOTICE_INVALID_REQUEST", "Notice request is invalid.", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> validation(ConstraintViolationException exception) {
        var response = buildErrorResponse("VALIDATION_ERROR", "Notice fields are invalid.", HttpStatus.BAD_REQUEST);
        response.getBody().setErrors(exception.getConstraintViolations().stream()
                .map(NoticeExceptionHandler::fieldError).toList());
        return response;
    }

    @ExceptionHandler(InvalidStorageKeyException.class)
    public ResponseEntity<ApiErrorResponse> invalidImage(InvalidStorageKeyException exception) {
        return buildErrorResponse("INVALID_IMAGE", "Image key is invalid.", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(StorageObjectNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> imageNotFound(StorageObjectNotFoundException exception) {
        return buildErrorResponse("IMAGE_NOT_FOUND", "Image was not found.", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> persistenceConflict(DataIntegrityViolationException exception) {
        return buildErrorResponse("NOTICE_CONFLICT", "Notice could not be saved due to a conflict.", HttpStatus.CONFLICT);
    }

    private static ValidationFieldErrorResponse fieldError(ConstraintViolation<?> violation) {
        String code = violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName()
                .replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
        return new ValidationFieldErrorResponse(violation.getPropertyPath().toString(), code, violation.getMessage());
    }
}
