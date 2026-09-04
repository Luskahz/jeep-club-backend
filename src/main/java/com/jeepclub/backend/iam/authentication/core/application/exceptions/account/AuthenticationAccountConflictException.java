package com.jeepclub.backend.iam.authentication.core.application.exceptions.account;

public class AuthenticationAccountConflictException extends RuntimeException {

    public AuthenticationAccountConflictException(Throwable cause) {
        super("Authentication account already exists for identity.", cause);
    }
}
