package com.jeepclub.backend.identity.api.module.exception;

public class IdentityRegistrationConflictException extends RuntimeException {

    public IdentityRegistrationConflictException(Throwable cause) {
        super("CPF, email or RG is already registered.", cause);
    }
}
