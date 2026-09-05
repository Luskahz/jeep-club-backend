package com.jeepclub.backend.iam.authentication.core.domain.exception.session;

public class SessionMissingTtlException extends RuntimeException {
    public SessionMissingTtlException(String message) {
        super(message);
    }
}
