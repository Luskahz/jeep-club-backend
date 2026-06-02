package com.jeepclub.backend.billing.core.domain.exception.cycle;

public class ChargeCycleCannotBeCanceledException extends RuntimeException {

    public ChargeCycleCannotBeCanceledException() {
        super("Charge cycle cannot be canceled in its current status.");
    }

    public ChargeCycleCannotBeCanceledException(String message) {
        super(message);
    }
}