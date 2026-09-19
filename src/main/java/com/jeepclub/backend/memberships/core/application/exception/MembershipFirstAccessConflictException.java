package com.jeepclub.backend.memberships.core.application.exception;

public class MembershipFirstAccessConflictException extends RuntimeException {
    public MembershipFirstAccessConflictException(String message) {
        super(message);
    }
}
