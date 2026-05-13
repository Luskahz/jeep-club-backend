package com.jeepclub.backend.authentication.core.domain.exception;

public class UserNotLockoutException extends RuntimeException {
    public UserNotLockoutException(String message) {
        super(message);
    }
}
