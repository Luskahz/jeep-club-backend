package com.jeepclub.backend.billing.core.domain.exception.charge;

public class InvalidMemberChargeStateException extends RuntimeException {
    public InvalidMemberChargeStateException(String message) {
        super(message);
    }
}
