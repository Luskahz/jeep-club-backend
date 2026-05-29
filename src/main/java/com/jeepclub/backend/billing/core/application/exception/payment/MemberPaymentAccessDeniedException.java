package com.jeepclub.backend.billing.core.application.exception.payment;

public class MemberPaymentAccessDeniedException extends RuntimeException {
    public MemberPaymentAccessDeniedException(String message) {
        super(message);
    }
}
