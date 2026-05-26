package com.jeepclub.backend.billing.core.application.exception.chargeCycle;

public class InactiveChargeDefinitionException extends RuntimeException {
    public InactiveChargeDefinitionException(String message) {
        super(message);
    }
}
