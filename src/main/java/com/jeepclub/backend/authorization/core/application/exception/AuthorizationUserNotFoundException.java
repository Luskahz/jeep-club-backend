package com.jeepclub.backend.authorization.core.application.exception;

public class AuthorizationUserNotFoundException extends RuntimeException {

    public AuthorizationUserNotFoundException(Long userId) {
        super("User not found with id: " + userId);
    }
}
