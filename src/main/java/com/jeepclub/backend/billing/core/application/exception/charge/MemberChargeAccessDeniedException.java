package com.jeepclub.backend.billing.core.application.exception.charge;

public class MemberChargeAccessDeniedException extends RuntimeException {
    public MemberChargeAccessDeniedException(String message) {
        super(message);
    }
}
