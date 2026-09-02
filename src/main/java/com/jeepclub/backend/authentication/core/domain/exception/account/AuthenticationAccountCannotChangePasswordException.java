package com.jeepclub.backend.authentication.core.domain.exception.account;

public class AuthenticationAccountCannotChangePasswordException extends IllegalStateException {

    public AuthenticationAccountCannotChangePasswordException() {
        super("Authentication account cannot request password change.");
    }
}
