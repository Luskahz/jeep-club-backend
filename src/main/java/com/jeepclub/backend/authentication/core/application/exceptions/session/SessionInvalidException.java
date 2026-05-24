package com.jeepclub.backend.authentication.core.application.exceptions.session;

public class SessionInvalidException extends RuntimeException {
    public SessionInvalidException(String message) {
        super(message);
    }
}
