package com.jeepclub.backend.memberships.api.http.exception;

import com.jeepclub.backend.memberships.api.module.exception.MembershipAccessUnavailableException;
import com.jeepclub.backend.memberships.api.module.exception.MembershipChargeUnavailableException;
import com.jeepclub.backend.memberships.api.module.exception.MembershipPaymentRequiredException;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MembershipAccessExceptionHandler extends ApiExceptionHandler {

    @ExceptionHandler(MembershipPaymentRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handlePaymentRequired(
            MembershipPaymentRequiredException exception
    ) {
        return buildErrorResponse(
                "MEMBERSHIP_PAYMENT_REQUIRED",
                exception.getMessage(),
                HttpStatus.PAYMENT_REQUIRED
        );
    }

    @ExceptionHandler({
            MembershipChargeUnavailableException.class,
            MembershipAccessUnavailableException.class
    })
    public ResponseEntity<ApiErrorResponse> handleUnavailable(RuntimeException exception) {
        return buildErrorResponse(
                "MEMBERSHIP_CHARGE_UNAVAILABLE",
                exception.getMessage(),
                HttpStatus.SERVICE_UNAVAILABLE
        );
    }
}
