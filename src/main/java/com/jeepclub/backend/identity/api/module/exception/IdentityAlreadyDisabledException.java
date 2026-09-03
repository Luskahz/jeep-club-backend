package com.jeepclub.backend.identity.api.module.exception;

public class IdentityAlreadyDisabledException extends IllegalStateException {

    public IdentityAlreadyDisabledException(Long identityId, Throwable cause) {
        super("Identity is already disabled: " + identityId, cause);
    }
}
