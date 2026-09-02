package com.jeepclub.backend.authentication.core.domain.exception.account;

public class AuthenticationAccountPasswordChangeRequiredException extends IllegalStateException {

    public AuthenticationAccountPasswordChangeRequiredException() {
        super("Credential is not permanent.");
    }
}
