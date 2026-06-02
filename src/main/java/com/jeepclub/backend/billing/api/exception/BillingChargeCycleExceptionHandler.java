package com.jeepclub.backend.billing.api.exception;

import com.jeepclub.backend.billing.core.application.exception.cycle.ChargeCycleAlreadyExistsException;
import com.jeepclub.backend.billing.core.application.exception.cycle.ChargeCycleNotFoundException;
import com.jeepclub.backend.billing.core.application.exception.cycle.ChargeCycleWithoutAssignmentsException;
import com.jeepclub.backend.billing.core.domain.exception.cycle.ChargeCycleAlreadyCanceledException;
import com.jeepclub.backend.billing.core.domain.exception.cycle.ChargeCycleCannotBeArchivedException;
import com.jeepclub.backend.billing.core.domain.exception.cycle.ChargeCycleCannotBeCanceledException;
import com.jeepclub.backend.billing.core.domain.exception.cycle.ChargeCycleCannotBeFinishedException;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.billing")
public class BillingChargeCycleExceptionHandler extends ApiExceptionHandler {

    @ExceptionHandler(ChargeCycleAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleChargeCycleAlreadyExists(
            ChargeCycleAlreadyExistsException exception
    ) {
        return buildErrorResponse(
                "CHARGE_CYCLE_ALREADY_EXISTS",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(ChargeCycleNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleChargeCycleNotFound(
            ChargeCycleNotFoundException exception
    ) {
        return buildErrorResponse(
                "CHARGE_CYCLE_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(ChargeCycleWithoutAssignmentsException.class)
    public ResponseEntity<ApiErrorResponse> handleChargeCycleWithoutAssignments(
            ChargeCycleWithoutAssignmentsException exception
    ) {
        return buildErrorResponse(
                "CHARGE_CYCLE_WITHOUT_ASSIGNMENTS",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(ChargeCycleAlreadyCanceledException.class)
    public ResponseEntity<ApiErrorResponse> handleChargeCycleAlreadyCanceled(
            ChargeCycleAlreadyCanceledException exception
    ) {
        return buildErrorResponse(
                "CHARGE_CYCLE_ALREADY_CANCELED",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(ChargeCycleCannotBeCanceledException.class)
    public ResponseEntity<ApiErrorResponse> handleChargeCycleCannotBeCanceled(
            ChargeCycleCannotBeCanceledException exception
    ) {
        return buildErrorResponse(
                "CHARGE_CYCLE_CANNOT_BE_CANCELED",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(ChargeCycleCannotBeFinishedException.class)
    public ResponseEntity<ApiErrorResponse> handleChargeCycleCannotBeFinished(
            ChargeCycleCannotBeFinishedException exception
    ) {
        return buildErrorResponse(
                "CHARGE_CYCLE_CANNOT_BE_FINISHED",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(ChargeCycleCannotBeArchivedException.class)
    public ResponseEntity<ApiErrorResponse> handleChargeCycleCannotBeArchived(
            ChargeCycleCannotBeArchivedException exception
    ) {
        return buildErrorResponse(
                "CHARGE_CYCLE_CANNOT_BE_ARCHIVED",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }
}