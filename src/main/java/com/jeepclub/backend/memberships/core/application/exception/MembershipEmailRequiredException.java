package com.jeepclub.backend.memberships.core.application.exception;

public class MembershipEmailRequiredException extends RuntimeException {
    public MembershipEmailRequiredException() {
        super("An email address is required for membership activation links.");
    }
}
