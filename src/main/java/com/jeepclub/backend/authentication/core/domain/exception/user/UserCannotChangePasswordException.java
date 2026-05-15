package com.jeepclub.backend.authentication.core.domain.exception.user;

public class UserCannotChangePasswordException extends RuntimeException {
    public UserCannotChangePasswordException(String message) {
        super(message);
    }
}
