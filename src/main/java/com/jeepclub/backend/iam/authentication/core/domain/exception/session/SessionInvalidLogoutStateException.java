package com.jeepclub.backend.iam.authentication.core.domain.exception.session;

public class SessionInvalidLogoutStateException extends RuntimeException {
    public SessionInvalidLogoutStateException(String message) {
        super(message);
    }
}
