package com.jeepclub.backend.billing.api.exception;

import com.jeepclub.backend.billing.core.application.exception.payment.*;
import com.jeepclub.backend.billing.core.domain.exception.payment.InvalidMemberPaymentStateException;
import com.jeepclub.backend.billing.core.application.exception.refund.MemberPaymentAlreadyRefundedException;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.billing")
public class BillingMemberPaymentExceptionHandler extends ApiExceptionHandler {

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

    @ExceptionHandler(MemberPaymentAlreadyRefundedException.class)
    public ResponseEntity<ApiErrorResponse> handleMemberPaymentAlreadyRefunded(
            MemberPaymentAlreadyRefundedException exception
    ) {
        return buildErrorResponse(
                "MEMBER_PAYMENT_ALREADY_REFUNDED",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(InvalidMemberPaymentStateException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidMemberPaymentState(
            InvalidMemberPaymentStateException exception
    ) {
        return buildErrorResponse(
                "INVALID_MEMBER_PAYMENT_STATE",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(MemberPaymentAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleMemberPaymentAlreadyExists(
            MemberPaymentAlreadyExistsException exception
    ) {
        return buildErrorResponse(
                "MEMBER_PAYMENT_ALREADY_EXISTS",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }
}