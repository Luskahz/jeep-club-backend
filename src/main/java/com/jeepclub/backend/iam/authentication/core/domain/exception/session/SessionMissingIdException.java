package com.jeepclub.backend.iam.authentication.core.domain.exception.session;

public class SessionMissingIdException extends RuntimeException {
    public SessionMissingIdException(String message) {
        super(message);
    }
}
