package com.jeepclub.backend.iam.authentication.core.domain.exception.account;

public class AuthenticationAccountNotDisabledException extends IllegalStateException {

    public AuthenticationAccountNotDisabledException(Long identityId) {
        super("Authentication account is not disabled: " + identityId);
    }
}
