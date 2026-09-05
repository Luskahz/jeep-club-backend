package com.jeepclub.backend.iam.authentication.core.domain.exception.session;

public class SessionInvalidActiveStateException extends RuntimeException {
    public SessionInvalidActiveStateException(String message) {
        super(message);
    }
}
