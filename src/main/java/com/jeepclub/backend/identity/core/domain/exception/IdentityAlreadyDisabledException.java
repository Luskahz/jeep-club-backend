package com.jeepclub.backend.identity.core.domain.exception;

public class IdentityAlreadyDisabledException extends IllegalStateException {

    public IdentityAlreadyDisabledException(Long identityId) {
        super("Identity is already disabled: " + identityId);
    }
}
