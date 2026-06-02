package com.jeepclub.backend.billing.core.domain.exception.cycle;

public class ChargeCycleCannotBeArchivedException extends RuntimeException {

    public ChargeCycleCannotBeArchivedException() {
        super("Charge cycle cannot be archived in its current status.");
    }

    public ChargeCycleCannotBeArchivedException(String message) {
        super(message);
    }
}