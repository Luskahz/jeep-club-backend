package com.jeepclub.backend.authentication.core.application.exceptions.user;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long userId) {
        super("User not found with id: " + userId);
    }
    public UserNotFoundException(String message) {
        super("User not found");
    }
}