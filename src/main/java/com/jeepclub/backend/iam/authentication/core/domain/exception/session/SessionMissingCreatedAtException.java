package com.jeepclub.backend.iam.authentication.core.domain.exception.session;

public class SessionMissingCreatedAtException extends RuntimeException {
    public SessionMissingCreatedAtException(String message) {
        super(message);
    }
}
