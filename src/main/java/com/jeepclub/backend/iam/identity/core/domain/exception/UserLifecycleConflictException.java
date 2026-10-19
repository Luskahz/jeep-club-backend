package com.jeepclub.backend.iam.identity.core.domain.exception;

public class UserLifecycleConflictException extends RuntimeException {
    public UserLifecycleConflictException(String message) { super(message); }
}
