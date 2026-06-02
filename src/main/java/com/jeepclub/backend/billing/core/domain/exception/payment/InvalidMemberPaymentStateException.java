package com.jeepclub.backend.billing.core.domain.exception.payment;

public class InvalidMemberPaymentStateException extends RuntimeException {
    public InvalidMemberPaymentStateException(String message) {
        super(message);
    }
}
