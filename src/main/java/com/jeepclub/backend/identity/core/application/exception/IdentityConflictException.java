package com.jeepclub.backend.identity.core.application.exception;

public class IdentityConflictException extends RuntimeException {

    public IdentityConflictException(Throwable cause) {
        super("CPF, email or RG is already registered.", cause);
    }
}
