package com.jeepclub.backend.memberships.api.module.exception;

public class MembershipAccessUnavailableException extends RuntimeException {

    public MembershipAccessUnavailableException(Throwable cause) {
        super("Membership access could not be evaluated.", cause);
    }
}
