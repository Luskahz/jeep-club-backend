package com.jeepclub.backend.identity.core.domain.exception;

public class IdentityNotDisabledException extends IllegalStateException {

    public IdentityNotDisabledException(Long identityId) {
        super("Identity is not disabled: " + identityId);
    }
}
