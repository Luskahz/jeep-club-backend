package com.jeepclub.backend.authentication.core.domain.exception.account;

public class AuthenticationAccountAlreadyDisabledException extends IllegalStateException {

    public AuthenticationAccountAlreadyDisabledException(Long identityId) {
        super("Authentication account is already disabled: " + identityId);
    }
}
