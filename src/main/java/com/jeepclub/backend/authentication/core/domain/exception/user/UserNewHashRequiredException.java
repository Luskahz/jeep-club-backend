package com.jeepclub.backend.authentication.core.domain.exception.user;

public class UserNewHashRequiredException extends RuntimeException {
    public UserNewHashRequiredException(String message) {
        super(message);
    }
}
