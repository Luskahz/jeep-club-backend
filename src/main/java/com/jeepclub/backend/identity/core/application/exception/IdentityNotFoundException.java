package com.jeepclub.backend.identity.core.application.exception;

public class IdentityNotFoundException extends RuntimeException {

    public IdentityNotFoundException(Long identityId) {
        super("Identity not found with id: " + identityId);
    }
}
