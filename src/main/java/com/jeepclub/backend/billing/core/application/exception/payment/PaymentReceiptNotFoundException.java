package com.jeepclub.backend.billing.core.application.exception.payment;

public class PaymentReceiptNotFoundException extends RuntimeException {
    public PaymentReceiptNotFoundException(String message) {
        super(message);
    }
}
