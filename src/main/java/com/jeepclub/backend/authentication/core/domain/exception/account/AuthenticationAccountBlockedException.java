package com.jeepclub.backend.authentication.core.domain.exception.account;

public class AuthenticationAccountBlockedException extends IllegalStateException {

    public AuthenticationAccountBlockedException() {
        super("Authentication account cannot authenticate.");
    }
}
