package com.jeepclub.backend.billing.core.application.exception.memberPayment;

public class MemberPaymentAccessDeniedException extends RuntimeException {
    public MemberPaymentAccessDeniedException(String message) {
        super(message);
    }
}
