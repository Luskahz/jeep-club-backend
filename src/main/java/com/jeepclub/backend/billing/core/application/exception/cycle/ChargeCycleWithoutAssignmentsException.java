package com.jeepclub.backend.billing.core.application.exception.cycle;

public class ChargeCycleWithoutAssignmentsException extends RuntimeException {
    public ChargeCycleWithoutAssignmentsException(String message) {
        super(message);
    }
}
