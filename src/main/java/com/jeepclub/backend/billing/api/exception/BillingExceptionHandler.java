package com.jeepclub.backend.billing.api.exception;

import com.jeepclub.backend.billing.core.application.exception.chargeAssignment.ChargeAssignmentAlreadyExistsException;
import com.jeepclub.backend.billing.core.application.exception.chargeAssignment.ChargeAssignmentNotFoundException;
import com.jeepclub.backend.billing.core.application.exception.chargeDefinition.ChargeDefinitionAlreadyExistsException;
import com.jeepclub.backend.billing.core.application.exception.chargeDefinition.ChargeDefinitionNotFoundException;
import com.jeepclub.backend.infra.web.exception.ApiErrorResponse;
import com.jeepclub.backend.infra.web.exception.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice(basePackages = "com.jeepclub.backend.billing")
public class BillingExceptionHandler extends ApiExceptionHandler {

    @ExceptionHandler(ChargeDefinitionAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleChargeDefinitionAlreadyExists(
            ChargeDefinitionAlreadyExistsException exception
    ) {
        return buildErrorResponse(
                "CHARGE_DEFINITION_ALREADY_EXISTS",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(ChargeDefinitionNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleChargeDefinitionNotFound(
            ChargeDefinitionNotFoundException exception
    ) {
        return buildErrorResponse(
                "CHARGE_DEFINITION_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

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
}
