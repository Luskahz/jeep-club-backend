package com.jeepclub.backend.billing.core.application.exception.charge;

public class MemberChargeCannotUpdateFinalAmountException extends RuntimeException {
    public MemberChargeCannotUpdateFinalAmountException(String message) {
        super(message);
    }
}
