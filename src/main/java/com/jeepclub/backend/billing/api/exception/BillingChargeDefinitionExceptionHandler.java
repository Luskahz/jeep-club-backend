package com.jeepclub.backend.billing.api.exception;

import com.jeepclub.backend.billing.core.application.exception.assignment.ChargeDefinitionCannotChangeAssignmentsException;
import com.jeepclub.backend.billing.core.application.exception.cycle.InactiveChargeDefinitionException;
import com.jeepclub.backend.billing.core.application.exception.definition.ChargeDefinitionAlreadyExistsException;
import com.jeepclub.backend.billing.core.application.exception.definition.ChargeDefinitionNotFoundException;
import com.jeepclub.backend.billing.core.domain.exception.definition.ArchivedChargeDefinitionCannotBeActivatedException;
import com.jeepclub.backend.billing.core.domain.exception.definition.ArchivedChargeDefinitionCannotBeDeactivatedException;
import com.jeepclub.backend.billing.core.domain.exception.definition.ArchivedChargeDefinitionCannotBeUpdatedException;
import com.jeepclub.backend.billing.core.domain.exception.definition.ChargeDefinitionAlreadyArchivedException;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.billing")
public class BillingChargeDefinitionExceptionHandler extends ApiExceptionHandler {

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

    @ExceptionHandler(ArchivedChargeDefinitionCannotBeActivatedException.class)
    public ResponseEntity<ApiErrorResponse> handleArchivedChargeDefinitionCannotBeActivated(
            ArchivedChargeDefinitionCannotBeActivatedException exception
    ) {
        return buildErrorResponse(
                "ARCHIVED_CHARGE_DEFINITION_CANNOT_BE_ACTIVATED",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(ArchivedChargeDefinitionCannotBeDeactivatedException.class)
    public ResponseEntity<ApiErrorResponse> handleArchivedChargeDefinitionCannotBeDeactivated(
            ArchivedChargeDefinitionCannotBeDeactivatedException exception
    ) {
        return buildErrorResponse(
                "ARCHIVED_CHARGE_DEFINITION_CANNOT_BE_DEACTIVATED",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(ArchivedChargeDefinitionCannotBeUpdatedException.class)
    public ResponseEntity<ApiErrorResponse> handleArchivedChargeDefinitionCannotBeUpdated(
            ArchivedChargeDefinitionCannotBeUpdatedException exception
    ) {
        return buildErrorResponse(
                "ARCHIVED_CHARGE_DEFINITION_CANNOT_BE_UPDATED",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(ChargeDefinitionAlreadyArchivedException.class)
    public ResponseEntity<ApiErrorResponse> handleChargeDefinitionAlreadyArchived(
            ChargeDefinitionAlreadyArchivedException exception
    ) {
        return buildErrorResponse(
                "CHARGE_DEFINITION_ALREADY_ARCHIVED",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(ChargeDefinitionCannotChangeAssignmentsException.class)
    public ResponseEntity<ApiErrorResponse> handleChargeDefinitionCannotChangeAssignments(
            ChargeDefinitionCannotChangeAssignmentsException exception
    ) {
        return buildErrorResponse(
                "CHARGE_DEFINITION_CANNOT_CHANGE_ASSIGNMENTS",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(InactiveChargeDefinitionException.class)
    public ResponseEntity<ApiErrorResponse> handleInactiveChargeDefinition(
            InactiveChargeDefinitionException exception
    ) {
        return buildErrorResponse(
                "INACTIVE_CHARGE_DEFINITION",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }
}