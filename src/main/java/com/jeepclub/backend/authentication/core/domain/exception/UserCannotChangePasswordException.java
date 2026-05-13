package com.jeepclub.backend.authentication.core.domain.exception;

public class UserCannotChangePasswordException extends RuntimeException {
    public UserCannotChangePasswordException(String message) {
        super(message);
    }
}
