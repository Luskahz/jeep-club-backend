package com.jeepclub.backend.authentication.core.domain.exception;

public class UserNowInstantRequiredException extends RuntimeException {
    public UserNowInstantRequiredException(String message) {
        super(message);
    }
}
