package com.jeepclub.backend.billing.core.application.exception.payment;

public class MemberPaymentNotFoundException extends RuntimeException {
    public MemberPaymentNotFoundException(String message) {
        super(message);
    }
}
