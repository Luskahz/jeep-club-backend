package com.jeepclub.backend.billing.core.application.exception.assignment;

public class ChargeAssignmentAlreadyExistsException extends RuntimeException {
    public ChargeAssignmentAlreadyExistsException(String message) {
        super(message);
    }
}
