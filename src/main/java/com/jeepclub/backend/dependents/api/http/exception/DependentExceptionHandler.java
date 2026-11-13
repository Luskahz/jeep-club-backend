package com.jeepclub.backend.dependents.api.http.exception;

import com.jeepclub.backend.dependents.core.application.exception.DependentAccessDeniedException;
import com.jeepclub.backend.dependents.core.application.exception.DependentCpfAlreadyInUseException;
import com.jeepclub.backend.dependents.core.application.exception.DependentNotFoundException;
import com.jeepclub.backend.dependents.core.application.exception.DependentOwnerInactiveException;
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
    @ExceptionHandler(com.jeepclub.backend.dependents.core.application.exception.InvalidDependentSearchException.class)
    public ResponseEntity<ApiErrorResponse> invalidSearch(Exception failure) {
        return buildErrorResponse("DEPENDENT_INVALID_SEARCH", failure.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            jakarta.validation.ConstraintViolationException exception
    ) {
        ResponseEntity<ApiErrorResponse> response = buildErrorResponse(
                "VALIDATION_ERROR",
                "The request contains invalid fields.",
                HttpStatus.BAD_REQUEST
        );
        response.getBody().setErrors(
                exception.getConstraintViolations().stream()
                        .map(DependentExceptionHandler::toFieldErrorResponse)
                        .toList()
        );
        return response;
    }

    private static com.jeepclub.backend.platform.web.exception.ValidationFieldErrorResponse toFieldErrorResponse(
            jakarta.validation.ConstraintViolation<?> violation
    ) {
        String field = violation.getPropertyPath().toString();
        String constraintCode = violation.getConstraintDescriptor()
                .getAnnotation()
                .annotationType()
                .getSimpleName();
        String responseCode = constraintCode
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toUpperCase(java.util.Locale.ROOT);

        return new com.jeepclub.backend.platform.web.exception.ValidationFieldErrorResponse(
                field,
                responseCode,
                violation.getMessage()
        );
    }

    @ExceptionHandler(org.springframework.dao.ConcurrencyFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleConcurrentMutation(org.springframework.dao.ConcurrencyFailureException exception) {
        return buildErrorResponse("DEPENDENT_CONCURRENT_MODIFICATION", "Concurrent modification; retry the operation.", HttpStatus.CONFLICT);
    }

    @ExceptionHandler(com.jeepclub.backend.dependents.core.domain.exception.DependentLifecycleConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleLifecycleConflict(com.jeepclub.backend.dependents.core.domain.exception.DependentLifecycleConflictException exception) {
        return buildErrorResponse("DEPENDENT_LIFECYCLE_CONFLICT", exception.getMessage(), HttpStatus.CONFLICT);
    }


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

    @ExceptionHandler(DependentOwnerInactiveException.class)
    public ResponseEntity<ApiErrorResponse> handleDependentOwnerInactive(
            DependentOwnerInactiveException exception
    ) {
        return buildErrorResponse(
                "DEPENDENT_OWNER_INACTIVE",
                exception.getMessage(),
                HttpStatus.CONFLICT
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
