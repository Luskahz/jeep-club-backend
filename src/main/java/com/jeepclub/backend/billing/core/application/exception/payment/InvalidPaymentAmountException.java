package com.jeepclub.backend.billing.core.application.exception.payment;

public class InvalidPaymentAmountException extends RuntimeException {
    public InvalidPaymentAmountException(String message) {
        super(message);
    }
}
