package com.jeepclub.backend.billing.core.application.exception.charge;

public class MemberChargeNotFoundException extends RuntimeException {
    public MemberChargeNotFoundException(String message) {
        super(message);
    }
}
