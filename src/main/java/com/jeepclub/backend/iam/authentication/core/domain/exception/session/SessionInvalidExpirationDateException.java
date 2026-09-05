package com.jeepclub.backend.iam.authentication.core.domain.exception.session;

public class SessionInvalidExpirationDateException extends RuntimeException {
    public SessionInvalidExpirationDateException(String message) {
        super(message);
    }
}
