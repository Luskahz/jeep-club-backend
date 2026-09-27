package com.jeepclub.backend.publications.api.http.exception;

import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import com.jeepclub.backend.publications.api.http.controller.member.PublicationFeedController;
import com.jeepclub.backend.publications.core.domain.exception.PublicationNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = PublicationFeedController.class)
public class PublicationFeedExceptionHandler extends ApiExceptionHandler {
    @ExceptionHandler(PublicationNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> notFound(Exception ignored) {
        return buildErrorResponse("PUBLICATION_NOT_FOUND", "Publication was not found.", HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> invalid(Exception ignored) {
        return buildErrorResponse("PUBLICATION_FEED_INVALID", "Feed filters are invalid.", HttpStatus.BAD_REQUEST);
    }
}
