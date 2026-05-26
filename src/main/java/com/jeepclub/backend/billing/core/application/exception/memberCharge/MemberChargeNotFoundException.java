package com.jeepclub.backend.billing.core.application.exception.memberCharge;

public class MemberChargeNotFoundException extends RuntimeException {
    public MemberChargeNotFoundException(String message) {
        super(message);
    }
}
