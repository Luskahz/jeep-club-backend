package com.jeepclub.backend.authentication.core.application.exceptions.session;

public class SessionInvalid extends RuntimeException {
    public SessionInvalid(String message) {
        super(message);
    }
}
