package com.jeepclub.backend.authentication.core.domain.exception;

public class UserLockoutException extends RuntimeException {
    public UserLockoutException(String message) {
        super(message);
    }
}
