package com.jeepclub.backend.memberships.api.module.exception;

public class MembershipChargeUnavailableException extends RuntimeException {

    public MembershipChargeUnavailableException() {
        super("Membership charge could not be resolved.");
    }
}
