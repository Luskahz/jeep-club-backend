package com.jeepclub.backend.memberships.core.application.exception;

public class MembershipBillingConfigurationNotFoundException extends RuntimeException {

    public MembershipBillingConfigurationNotFoundException() {
        super("Membership billing configuration has not been created.");
    }
}
