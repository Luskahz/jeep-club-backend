package com.jeepclub.backend.authentication.core.domain.exception;

public class UserBlockedForLoginException extends RuntimeException {
    public UserBlockedForLoginException(String message) {
        super(message);
    }
}
