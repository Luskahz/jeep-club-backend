package com.jeepclub.backend.authorization.core.application.exception.userrole;

public class AuthorizationUserNotFoundException extends RuntimeException {

    public AuthorizationUserNotFoundException(Long userId) {
        super("User not found with id: " + userId);
    }
}
