package com.jeepclub.backend.billing.core.application.exception.chargeAssignment;

public class ChargeAssignmentAlreadyExistsException extends RuntimeException {
    public ChargeAssignmentAlreadyExistsException(String message) {
        super(message);
    }
}
