package com.jeepclub.backend.authentication.core.domain.exception.account;

public class AuthenticationAccountHashRequiredException extends IllegalArgumentException {

    public AuthenticationAccountHashRequiredException() {
        super("Password hash is required.");
    }
}
