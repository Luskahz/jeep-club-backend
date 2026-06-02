package com.jeepclub.backend.billing.core.domain.exception.assignment;

public class ChargeAssignmentAlreadyInactiveException extends RuntimeException {

    public ChargeAssignmentAlreadyInactiveException() {
        super("Charge assignment is already inactive.");
    }
}