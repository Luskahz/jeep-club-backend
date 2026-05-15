package com.jeepclub.backend.authentication.core.domain.exception.user;

public class UserIdRequiredException extends RuntimeException {
    public UserIdRequiredException(String message) {
        super(message);
    }
}
