package com.jeepclub.backend.billing.core.application.exception.refund;

public class MemberRefundAccessDeniedException extends RuntimeException {
    public MemberRefundAccessDeniedException(String message) {
        super(message);
    }
}
