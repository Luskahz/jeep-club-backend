package com.jeepclub.backend.identity.api.module.exception;

public class UserRegistrationConflictException extends RuntimeException {

    public UserRegistrationConflictException(Throwable cause) {
        super("CPF, email or RG is already registered.", cause);
    }
}
