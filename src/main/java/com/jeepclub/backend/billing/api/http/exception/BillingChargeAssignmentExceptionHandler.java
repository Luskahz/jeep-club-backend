package com.jeepclub.backend.billing.api.http.exception;

import com.jeepclub.backend.billing.core.application.exception.assignment.BillingAssignmentTargetNotFoundException;
import com.jeepclub.backend.billing.core.application.exception.assignment.ChargeAssignmentAlreadyExistsException;
import com.jeepclub.backend.billing.core.application.exception.assignment.ChargeAssignmentNotFoundException;
import com.jeepclub.backend.billing.core.domain.exception.assignment.ChargeAssignmentAlreadyActiveException;
import com.jeepclub.backend.billing.core.domain.exception.assignment.ChargeAssignmentAlreadyInactiveException;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.billing")
public class BillingChargeAssignmentExceptionHandler extends ApiExceptionHandler {

    @ExceptionHandler(ChargeAssignmentAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleChargeAssignmentAlreadyExists(
            ChargeAssignmentAlreadyExistsException exception
    ) {
        return buildErrorResponse(
                "CHARGE_ASSIGNMENT_ALREADY_EXISTS",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(ChargeAssignmentNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleChargeAssignmentNotFound(
            ChargeAssignmentNotFoundException exception
    ) {
        return buildErrorResponse(
                "CHARGE_ASSIGNMENT_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(BillingAssignmentTargetNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleBillingAssignmentTargetNotFound(
            BillingAssignmentTargetNotFoundException exception
    ) {
        return buildErrorResponse(
                "BILLING_ASSIGNMENT_TARGET_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(ChargeAssignmentAlreadyActiveException.class)
    public ResponseEntity<ApiErrorResponse> handleChargeAssignmentAlreadyActive(
            ChargeAssignmentAlreadyActiveException exception
    ) {
        return buildErrorResponse(
                "CHARGE_ASSIGNMENT_ALREADY_ACTIVE",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(ChargeAssignmentAlreadyInactiveException.class)
    public ResponseEntity<ApiErrorResponse> handleChargeAssignmentAlreadyInactive(
            ChargeAssignmentAlreadyInactiveException exception
    ) {
        return buildErrorResponse(
                "CHARGE_ASSIGNMENT_ALREADY_INACTIVE",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }
}