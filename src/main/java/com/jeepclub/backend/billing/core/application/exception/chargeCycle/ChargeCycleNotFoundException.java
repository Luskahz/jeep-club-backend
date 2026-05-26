package com.jeepclub.backend.billing.core.application.exception.chargeCycle;

public class ChargeCycleNotFoundException extends RuntimeException {
    public ChargeCycleNotFoundException(String message) {
        super(message);
    }
}
