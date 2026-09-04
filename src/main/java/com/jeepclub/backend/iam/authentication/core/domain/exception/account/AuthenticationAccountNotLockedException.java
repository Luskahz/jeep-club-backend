package com.jeepclub.backend.iam.authentication.core.domain.exception.account;

public class AuthenticationAccountNotLockedException extends IllegalStateException {

    public AuthenticationAccountNotLockedException() {
        super("Authentication account is not locked.");
    }
}
