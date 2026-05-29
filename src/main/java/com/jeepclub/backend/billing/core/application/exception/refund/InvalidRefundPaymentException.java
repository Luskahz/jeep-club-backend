package com.jeepclub.backend.billing.core.application.exception.refund;

public class InvalidRefundPaymentException extends RuntimeException {
    public InvalidRefundPaymentException(String message) {
        super(message);
    }
}
