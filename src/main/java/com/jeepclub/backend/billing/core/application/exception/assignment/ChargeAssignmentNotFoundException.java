package com.jeepclub.backend.billing.core.application.exception.assignment;

public class ChargeAssignmentNotFoundException extends RuntimeException {
    public ChargeAssignmentNotFoundException(String message) {
        super(message);
    }
}
