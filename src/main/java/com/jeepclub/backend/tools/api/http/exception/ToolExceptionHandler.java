package com.jeepclub.backend.tools.api.http.exception;

import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import com.jeepclub.backend.tools.core.application.exception.ToolNotFoundException;
import com.jeepclub.backend.tools.core.domain.exception.InvalidToolStatusException;
import com.jeepclub.backend.tools.core.domain.exception.ToolAccessDeniedException;
import com.jeepclub.backend.tools.core.domain.exception.ToolAlreadyDeletedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.tools")
public class ToolExceptionHandler extends ApiExceptionHandler {

    @ExceptionHandler(ToolNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleToolNotFound(ToolNotFoundException exception) {
        return buildErrorResponse("TOOL_NOT_FOUND", exception.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ToolAccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleToolAccessDenied(ToolAccessDeniedException exception) {
        return buildErrorResponse("TOOL_ACCESS_DENIED", exception.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(InvalidToolStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidToolStatus(InvalidToolStatusException exception) {
        return buildErrorResponse("INVALID_TOOL_STATUS", exception.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ToolAlreadyDeletedException.class)
    public ResponseEntity<ApiErrorResponse> handleToolAlreadyDeleted(
            ToolAlreadyDeletedException exception
    ) {
        return buildErrorResponse(
                "TOOL_ALREADY_DELETED",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }
}
