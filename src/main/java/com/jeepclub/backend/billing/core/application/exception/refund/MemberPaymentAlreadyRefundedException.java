package com.jeepclub.backend.billing.core.application.exception.refund;

public class MemberPaymentAlreadyRefundedException extends RuntimeException {

    public MemberPaymentAlreadyRefundedException(String message) {
        super(message);
    }
}