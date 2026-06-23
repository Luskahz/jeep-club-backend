package com.jeepclub.backend.billing.api.http.exception;

import com.jeepclub.backend.billing.core.application.exception.refund.InvalidRefundPaymentException;
import com.jeepclub.backend.billing.core.application.exception.refund.MemberRefundAccessDeniedException;
import com.jeepclub.backend.billing.core.application.exception.refund.MemberRefundNotFoundException;
import com.jeepclub.backend.billing.core.domain.exception.refund.InvalidMemberRefundStateException;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.billing")
public class BillingMemberRefundExceptionHandler extends ApiExceptionHandler {

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

    @ExceptionHandler(InvalidRefundPaymentException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRefundPayment(
            InvalidRefundPaymentException exception
    ) {
        return buildErrorResponse(
                "INVALID_REFUND_PAYMENT",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(InvalidMemberRefundStateException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidMemberRefundState(
            InvalidMemberRefundStateException exception
    ) {
        return buildErrorResponse(
                "INVALID_MEMBER_REFUND_STATE",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }
}