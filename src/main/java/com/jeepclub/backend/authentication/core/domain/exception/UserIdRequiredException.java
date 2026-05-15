package com.jeepclub.backend.authentication.core.domain.exception;

public class UserIdRequiredException extends RuntimeException {
    public UserIdRequiredException(String message) {
        super(message);
    }
}
