package com.jeepclub.backend.billing.api.exception;

import com.jeepclub.backend.billing.core.application.exception.assignment.BillingAssignmentTargetNotFoundException;
import com.jeepclub.backend.billing.core.application.exception.assignment.ChargeAssignmentAlreadyExistsException;
import com.jeepclub.backend.billing.core.application.exception.assignment.ChargeAssignmentNotFoundException;
import com.jeepclub.backend.billing.core.application.exception.assignment.ChargeDefinitionCannotChangeAssignmentsException;
import com.jeepclub.backend.billing.core.application.exception.cycle.ChargeCycleAlreadyExistsException;
import com.jeepclub.backend.billing.core.application.exception.cycle.ChargeCycleNotFoundException;
import com.jeepclub.backend.billing.core.application.exception.cycle.ChargeCycleWithoutAssignmentsException;
import com.jeepclub.backend.billing.core.application.exception.cycle.InactiveChargeDefinitionException;
import com.jeepclub.backend.billing.core.application.exception.definition.ChargeDefinitionAlreadyExistsException;
import com.jeepclub.backend.billing.core.application.exception.definition.ChargeDefinitionNotFoundException;
import com.jeepclub.backend.billing.core.application.exception.charge.MemberChargeAccessDeniedException;
import com.jeepclub.backend.billing.core.application.exception.charge.MemberChargeNotFoundException;
import com.jeepclub.backend.billing.core.application.exception.payment.InvalidPaymentAmountException;
import com.jeepclub.backend.billing.core.application.exception.payment.InvalidPaymentReceiptException;
import com.jeepclub.backend.billing.core.application.exception.payment.MemberPaymentAccessDeniedException;
import com.jeepclub.backend.billing.core.application.exception.payment.MemberPaymentNotFoundException;
import com.jeepclub.backend.billing.core.application.exception.refund.MemberRefundAccessDeniedException;
import com.jeepclub.backend.billing.core.application.exception.refund.MemberRefundNotFoundException;
import com.jeepclub.backend.billing.core.domain.exception.assignment.ChargeAssignmentAlreadyActiveException;
import com.jeepclub.backend.billing.core.domain.exception.assignment.ChargeAssignmentAlreadyInactiveException;
import com.jeepclub.backend.billing.core.domain.exception.cycle.ChargeCycleAlreadyCanceledException;
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

    @ExceptionHandler(MemberRefundNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleMemberRefundNotFound(
            MemberRefundNotFoundException exception
    ) {
        return buildErrorResponse(
                "MEMBER_REFUND_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(MemberRefundAccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleMemberRefundAccessDenied(
            MemberRefundAccessDeniedException exception
    ) {
        return buildErrorResponse(
                "MEMBER_REFUND_ACCESS_DENIED",
                exception.getMessage(),
                HttpStatus.FORBIDDEN
        );
    }
}

