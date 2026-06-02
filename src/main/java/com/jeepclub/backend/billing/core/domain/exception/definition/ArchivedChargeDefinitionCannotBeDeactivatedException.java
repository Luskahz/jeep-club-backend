package com.jeepclub.backend.billing.core.domain.exception.definition;

public class ArchivedChargeDefinitionCannotBeDeactivatedException extends RuntimeException {

    public ArchivedChargeDefinitionCannotBeDeactivatedException() {
        super("Archived charge definition cannot be deactivated.");
    }

    public ArchivedChargeDefinitionCannotBeDeactivatedException(String message) {
        super(message);
    }
}