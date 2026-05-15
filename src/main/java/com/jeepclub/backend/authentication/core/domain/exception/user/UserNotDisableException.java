package com.jeepclub.backend.authentication.core.domain.exception.user;

public class UserNotDisableException extends RuntimeException {
    public UserNotDisableException(String message) {
        super(message);
    }
}
