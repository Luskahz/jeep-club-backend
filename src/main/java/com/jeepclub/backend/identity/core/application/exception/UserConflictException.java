package com.jeepclub.backend.identity.core.application.exception;

public class UserConflictException extends RuntimeException {

    public UserConflictException(Throwable cause) {
        super("CPF, email or RG is already registered.", cause);
    }
}
