package com.jeepclub.backend.billing.core.application.exception.payment;

public class MemberPaymentAlreadyExistsException extends RuntimeException {
    public MemberPaymentAlreadyExistsException(String message) {
        super(message);
    }
}
