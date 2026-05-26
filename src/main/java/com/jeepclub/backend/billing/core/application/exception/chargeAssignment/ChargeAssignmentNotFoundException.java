package com.jeepclub.backend.billing.core.application.exception.chargeAssignment;

public class ChargeAssignmentNotFoundException extends RuntimeException {
    public ChargeAssignmentNotFoundException(String message) {
        super(message);
    }
}
