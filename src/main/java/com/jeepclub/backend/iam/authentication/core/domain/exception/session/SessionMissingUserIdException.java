package com.jeepclub.backend.iam.authentication.core.domain.exception.session;

public class SessionMissingUserIdException extends RuntimeException {
    public SessionMissingUserIdException(String message) {
        super(message);
    }
}
