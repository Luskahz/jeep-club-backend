package com.jeepclub.backend.billing.core.application.exception.cycle;

public class InactiveChargeDefinitionException extends RuntimeException {
    public InactiveChargeDefinitionException(String message) {
        super(message);
    }
}
