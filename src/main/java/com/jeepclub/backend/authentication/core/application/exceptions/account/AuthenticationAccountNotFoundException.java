package com.jeepclub.backend.authentication.core.application.exceptions.account;

public class AuthenticationAccountNotFoundException extends IllegalStateException {

    public AuthenticationAccountNotFoundException(String message) {
        super(message);
    }

    public AuthenticationAccountNotFoundException(Long userId) {
        super("Authentication account not found for user id: " + userId);
    }
}
