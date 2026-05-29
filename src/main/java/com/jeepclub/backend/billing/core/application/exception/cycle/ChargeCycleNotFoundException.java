package com.jeepclub.backend.billing.core.application.exception.cycle;

public class ChargeCycleNotFoundException extends RuntimeException {
    public ChargeCycleNotFoundException(String message) {
        super(message);
    }
}
