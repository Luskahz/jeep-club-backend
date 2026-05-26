package com.jeepclub.backend.billing.core.application.exception.chargeDefinition;

public class ChargeDefinitionAlreadyExistsException extends RuntimeException {
    public ChargeDefinitionAlreadyExistsException(String message) {
        super(message);
    }
}
