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
    public ResponseEntity<ApiErrorResponse> conflict(Exception ignored) {
        return buildErrorResponse("PUBLICATION_LIKE_CONFLICT", "Concurrent publication interaction conflict.", HttpStatus.CONFLICT);
    }
}
