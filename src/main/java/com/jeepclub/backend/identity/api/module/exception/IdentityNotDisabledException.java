package com.jeepclub.backend.identity.api.module.exception;

public class IdentityNotDisabledException extends IllegalStateException {

    public IdentityNotDisabledException(Long identityId, Throwable cause) {
        super("Identity is not disabled: " + identityId, cause);
    }
}
