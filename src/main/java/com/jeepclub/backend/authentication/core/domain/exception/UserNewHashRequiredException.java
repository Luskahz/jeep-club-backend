package com.jeepclub.backend.authentication.core.domain.exception;

public class UserNewHashRequiredException extends RuntimeException {
    public UserNewHashRequiredException(String message) {
        super(message);
    }
}
