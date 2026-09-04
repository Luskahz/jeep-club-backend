package com.jeepclub.backend.iam.authentication.core.domain.exception.session;

public class SessionNowInstantRequiredException extends RuntimeException {
    public SessionNowInstantRequiredException(String message) {
        super(message);
    }
}
