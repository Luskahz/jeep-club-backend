package com.jeepclub.backend.authentication.core.domain.exception.user;

public class UserBlockedForLoginException extends RuntimeException {
    public UserBlockedForLoginException(String message) {
        super(message);
    }
}
