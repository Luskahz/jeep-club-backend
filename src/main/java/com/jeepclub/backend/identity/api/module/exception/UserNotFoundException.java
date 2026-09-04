package com.jeepclub.backend.identity.api.module.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long identityId) {
        super("User not found with id: " + identityId);
    }
}
