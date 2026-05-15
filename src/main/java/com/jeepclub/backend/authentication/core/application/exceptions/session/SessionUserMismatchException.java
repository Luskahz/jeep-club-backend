package com.jeepclub.backend.authentication.core.application.exceptions.session;

public class SessionUserMismatchException extends RuntimeException {
    public SessionUserMismatchException(String message) {
        super(message);
    }
}
