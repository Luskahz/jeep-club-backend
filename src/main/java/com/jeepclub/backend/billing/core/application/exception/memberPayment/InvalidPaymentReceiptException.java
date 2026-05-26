package com.jeepclub.backend.billing.core.application.exception.memberPayment;

public class InvalidPaymentReceiptException extends RuntimeException {
    public InvalidPaymentReceiptException(String message) {
        super(message);
    }
}
