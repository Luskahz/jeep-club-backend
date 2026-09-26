package com.jeepclub.backend.memberships.core.application.exception;

public class InvalidMembershipChargeDefinitionException extends RuntimeException {

    public InvalidMembershipChargeDefinitionException() {
        super("Charge definition is not available for membership billing.");
    }
}
