package com.jeepclub.backend.publications.api.http.exception;

import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import com.jeepclub.backend.publications.api.http.controller.member.PublicationSocialController;
import com.jeepclub.backend.publications.core.domain.exception.PublicationNotFoundException;
import com.jeepclub.backend.shared.storage.exception.InvalidStorageKeyException;
import com.jeepclub.backend.shared.storage.exception.StorageObjectNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = PublicationSocialController.class)
public class PublicationSocialExceptionHandler extends ApiExceptionHandler {
    private final com.jeepclub.backend.platform.web.exception.GlobalExceptionHandler global;
    public PublicationSocialExceptionHandler(com.jeepclub.backend.platform.web.exception.GlobalExceptionHandler global) {this.global=global;}
    @ExceptionHandler(PublicationNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> notFound(Exception ignored) {
        return buildErrorResponse("PUBLICATION_NOT_FOUND", "Publication was not found.", HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler({IllegalArgumentException.class, InvalidStorageKeyException.class})
    public ResponseEntity<ApiErrorResponse> invalid(Exception ignored) {
        return buildErrorResponse("PUBLICATION_COMMENT_INVALID", "Comment or image data is invalid.", HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(StorageObjectNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> missingImage(Exception ignored) {
        return buildErrorResponse("PUBLICATION_COMMENT_IMAGE_NOT_FOUND", "Comment image was not found.", HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<? extends org.springframework.http.ProblemDetail> conflict(DataIntegrityViolationException failure) {
        if (!PublicationConstraintConflicts.matches(failure, java.util.Set.of("uk_publication_like_member")))
            return ResponseEntity.internalServerError().body(global.handleUnexpectedException(failure,org.springframework.context.i18n.LocaleContextHolder.getLocale()));
        return buildErrorResponse("PUBLICATION_LIKE_CONFLICT", "Concurrent publication interaction conflict.", HttpStatus.CONFLICT);
    }
}
