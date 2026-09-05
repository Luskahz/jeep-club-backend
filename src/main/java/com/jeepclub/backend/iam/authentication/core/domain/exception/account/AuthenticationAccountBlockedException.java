package com.jeepclub.backend.iam.authentication.core.domain.exception.account;

public class AuthenticationAccountBlockedException extends IllegalStateException {

    public AuthenticationAccountBlockedException() {
        super("Authentication account cannot authenticate.");
    }
}
