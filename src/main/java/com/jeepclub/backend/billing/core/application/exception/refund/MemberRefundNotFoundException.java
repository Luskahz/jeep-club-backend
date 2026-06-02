package com.jeepclub.backend.billing.core.application.exception.refund;

public class MemberRefundNotFoundException extends RuntimeException {

    public MemberRefundNotFoundException(String message) {
        super(message);
    }
}