package com.jeepclub.backend.iam.authentication.core.domain.exception.session;

public class SessionNotActiveException extends RuntimeException {
    public SessionNotActiveException(String message) {
        super(message);
    }
}
