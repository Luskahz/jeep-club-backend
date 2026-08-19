package com.jeepclub.backend.dependents.core.application.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long socioId) {
        super("User not found: " + socioId);
    }
}