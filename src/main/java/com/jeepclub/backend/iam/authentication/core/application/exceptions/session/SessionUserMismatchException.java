package com.jeepclub.backend.iam.authentication.core.application.exceptions.session;

public class SessionUserMismatchException extends RuntimeException {
    public SessionUserMismatchException(String message) {
        super(message);
    }
}
