package com.jeepclub.backend.authentication.core.application.exceptions.session;

public class SessionNotFoundException extends RuntimeException {
    public SessionNotFoundException(String message) {
        super(message);
    }
}
