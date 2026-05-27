package com.jeepclub.backend.billing.core.domain.exception.definition;

public class ArchivedChargeDefinitionCannotBeUpdatedException extends RuntimeException {

    public ArchivedChargeDefinitionCannotBeUpdatedException() {
        super("Archived charge definition cannot be updated.");
    }

    public ArchivedChargeDefinitionCannotBeUpdatedException(String message) {
        super(message);
    }
}