package com.jeepclub.backend.dependents.api.http.exception;

import com.jeepclub.backend.dependents.core.application.exception.DependentAccessDeniedException;
import com.jeepclub.backend.dependents.core.application.exception.DependentCpfAlreadyInUseException;
import com.jeepclub.backend.dependents.core.application.exception.DependentNotFoundException;
import com.jeepclub.backend.dependents.core.application.exception.DependentOwnerNotFoundException;
import com.jeepclub.backend.dependents.core.domain.exception.DependentAlreadyDeletedException;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.dependents")
public class DependentExceptionHandler extends ApiExceptionHandler {

    @ExceptionHandler(DependentNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleDependentNotFound(
            DependentNotFoundException exception
    ) {
        return buildErrorResponse(
                "DEPENDENT_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(DependentOwnerNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleDependentOwnerNotFound(
            DependentOwnerNotFoundException exception
    ) {
        return buildErrorResponse(
                "DEPENDENT_OWNER_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(DependentAccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleDependentAccessDenied(
            DependentAccessDeniedException exception
    ) {
        return buildErrorResponse(
                "DEPENDENT_ACCESS_DENIED",
                exception.getMessage(),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(DependentCpfAlreadyInUseException.class)
    public ResponseEntity<ApiErrorResponse> handleDependentCpfAlreadyInUse(
            DependentCpfAlreadyInUseException exception
    ) {
        return buildErrorResponse(
                "DEPENDENT_CPF_ALREADY_IN_USE",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(DependentAlreadyDeletedException.class)
    public ResponseEntity<ApiErrorResponse> handleDependentAlreadyDeleted(
            DependentAlreadyDeletedException exception
    ) {
        return buildErrorResponse(
                "DEPENDENT_ALREADY_DELETED",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }
}
