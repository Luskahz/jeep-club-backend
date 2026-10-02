package com.jeepclub.backend.memberships.api.module.exception;

public class MembershipPaymentRequiredException extends RuntimeException {

    public MembershipPaymentRequiredException() {
        super("A valid membership payment is required.");
    }
}
