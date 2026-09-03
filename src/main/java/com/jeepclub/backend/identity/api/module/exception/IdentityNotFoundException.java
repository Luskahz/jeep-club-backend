package com.jeepclub.backend.identity.api.module.exception;

public class IdentityNotFoundException extends RuntimeException {

    public IdentityNotFoundException(Long identityId) {
        super("Identity not found with id: " + identityId);
    }
}
