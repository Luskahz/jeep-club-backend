package com.jeepclub.backend.billing.core.domain.exception.definition;

public class ChargeDefinitionAlreadyArchivedException extends RuntimeException {

    public ChargeDefinitionAlreadyArchivedException() {
        super("Charge definition is already archived.");
    }

    public ChargeDefinitionAlreadyArchivedException(String message) {
        super(message);
    }
}