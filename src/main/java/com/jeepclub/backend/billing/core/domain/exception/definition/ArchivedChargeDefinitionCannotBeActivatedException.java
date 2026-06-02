package com.jeepclub.backend.billing.core.domain.exception.definition;

public class ArchivedChargeDefinitionCannotBeActivatedException extends RuntimeException {

    public ArchivedChargeDefinitionCannotBeActivatedException() {
        super("Archived charge definition cannot be activated.");
    }

    public ArchivedChargeDefinitionCannotBeActivatedException(String message) {
        super(message);
    }
}