package com.jeepclub.backend.authentication.core.domain.exception.session;

public class SessionInvalidExpirationDateException extends RuntimeException {
    public SessionInvalidExpirationDateException(String message) {
        super(message);
    }
}
