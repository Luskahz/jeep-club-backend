package com.jeepclub.backend.billing.core.domain.exception.assignment;

public class ChargeAssignmentAlreadyActiveException extends RuntimeException {

    public ChargeAssignmentAlreadyActiveException() {
        super("Charge assignment is already active.");
    }
}