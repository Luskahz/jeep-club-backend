package com.jeepclub.backend.iam.authentication.core.domain.exception.session;

public class SessionInvalidTtlValueException extends RuntimeException {
    public SessionInvalidTtlValueException(String message) {
        super(message);
    }
}
