package com.jeepclub.backend.billing.api.exception;

import com.jeepclub.backend.billing.core.application.exception.chargeAssignment.ChargeAssignmentAlreadyExistsException;
import com.jeepclub.backend.billing.core.application.exception.chargeAssignment.ChargeAssignmentNotFoundException;
import com.jeepclub.backend.billing.core.application.exception.chargeDefinition.ChargeDefinitionAlreadyExistsException;
import com.jeepclub.backend.billing.core.application.exception.chargeDefinition.ChargeDefinitionNotFoundException;
import com.jeepclub.backend.billing.core.application.exception.memberCharge.MemberChargeAccessDeniedException;
import com.jeepclub.backend.billing.core.application.exception.memberCharge.MemberChargeNotFoundException;
import com.jeepclub.backend.billing.core.application.exception.memberPayment.InvalidPaymentAmountException;
import com.jeepclub.backend.billing.core.application.exception.memberPayment.InvalidPaymentReceiptException;
import com.jeepclub.backend.billing.core.application.exception.memberPayment.MemberPaymentAccessDeniedException;
import com.jeepclub.backend.billing.core.application.exception.memberPayment.MemberPaymentNotFoundException;
import com.jeepclub.backend.billing.core.domain.exception.definition.ArchivedChargeDefinitionCannotBeActivatedException;
import com.jeepclub.backend.billing.core.domain.exception.definition.ArchivedChargeDefinitionCannotBeDeactivatedException;
import com.jeepclub.backend.billing.core.domain.exception.definition.ArchivedChargeDefinitionCannotBeUpdatedException;
import com.jeepclub.backend.billing.core.domain.exception.definition.ChargeDefinitionAlreadyArchivedException;
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
    @ExceptionHandler(MemberChargeNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleMemberChargeNotFound(
            MemberChargeNotFoundException exception
    ) {
        return buildErrorResponse(
                "MEMBER_CHARGE_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(MemberChargeAccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleMemberChargeAccessDenied(
            MemberChargeAccessDeniedException exception
    ) {
        return buildErrorResponse(
                "MEMBER_CHARGE_ACCESS_DENIED",
                exception.getMessage(),
                HttpStatus.FORBIDDEN
        );
    }
    @ExceptionHandler(MemberPaymentNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleMemberPaymentNotFound(
            MemberPaymentNotFoundException exception
    ) {
        return buildErrorResponse(
                "MEMBER_PAYMENT_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(MemberPaymentAccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleMemberPaymentAccessDenied(
            MemberPaymentAccessDeniedException exception
    ) {
        return buildErrorResponse(
                "MEMBER_PAYMENT_ACCESS_DENIED",
                exception.getMessage(),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(InvalidPaymentAmountException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidPaymentAmount(
            InvalidPaymentAmountException exception
    ) {
        return buildErrorResponse(
                "INVALID_PAYMENT_AMOUNT",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(InvalidPaymentReceiptException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidPaymentReceipt(
            InvalidPaymentReceiptException exception
    ) {
        return buildErrorResponse(
                "INVALID_PAYMENT_RECEIPT",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
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
}
