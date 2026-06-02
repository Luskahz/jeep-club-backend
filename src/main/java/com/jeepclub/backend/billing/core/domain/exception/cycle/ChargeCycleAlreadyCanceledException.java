package com.jeepclub.backend.billing.core.domain.exception.cycle;

public class ChargeCycleAlreadyCanceledException extends RuntimeException {

    public ChargeCycleAlreadyCanceledException() {
        super("Charge cycle is already canceled.");
    }

    public ChargeCycleAlreadyCanceledException(String message) {
        super(message);
    }
}