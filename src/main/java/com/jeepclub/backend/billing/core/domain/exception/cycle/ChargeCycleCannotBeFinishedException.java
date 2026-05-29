package com.jeepclub.backend.billing.core.domain.exception.cycle;

public class ChargeCycleCannotBeFinishedException extends RuntimeException {

    public ChargeCycleCannotBeFinishedException() {
        super("Charge cycle cannot be finished in its current status.");
    }

    public ChargeCycleCannotBeFinishedException(String message) {
        super(message);
    }
}