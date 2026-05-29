package com.jeepclub.backend.billing.core.domain.exception.refund;

public class InvalidMemberRefundStateException extends RuntimeException {

    public InvalidMemberRefundStateException(String message) {
        super(message);
    }
}