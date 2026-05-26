package com.jeepclub.backend.billing.core.application.exception.chargeCycle;

public class ChargeCycleAlreadyExistsException extends RuntimeException {
    public ChargeCycleAlreadyExistsException(String message) {
        super(message);
    }
}
