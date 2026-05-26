package com.jeepclub.backend.billing.core.application.exception.chargeDefinition;

public class ChargeDefinitionNotFoundException extends RuntimeException {
    public ChargeDefinitionNotFoundException(String message) {
        super(message);
    }
}
