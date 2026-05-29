package com.jeepclub.backend.billing.api.exception;

import com.jeepclub.backend.billing.core.application.exception.charge.MemberChargeAccessDeniedException;
import com.jeepclub.backend.billing.core.application.exception.charge.MemberChargeNotFoundException;
import com.jeepclub.backend.billing.core.domain.exception.charge.InvalidMemberChargeStateException;
import com.jeepclub.backend.infra.web.exception.ApiErrorResponse;
import com.jeepclub.backend.infra.web.exception.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.billing")
public class BillingMemberChargeExceptionHandler extends ApiExceptionHandler {

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

    @ExceptionHandler(InvalidMemberChargeStateException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidMemberChargeState(
            InvalidMemberChargeStateException exception
    ) {
        return buildErrorResponse(
                "INVALID_MEMBER_CHARGE_STATE",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }
}