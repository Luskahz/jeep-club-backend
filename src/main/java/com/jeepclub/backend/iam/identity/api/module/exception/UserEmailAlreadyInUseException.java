package com.jeepclub.backend.iam.identity.api.module.exception;

public class UserEmailAlreadyInUseException extends RuntimeException {
    public UserEmailAlreadyInUseException() {
        super("Email is already registered by another user.");
    }

    public UserEmailAlreadyInUseException(Throwable cause) {
        super("Email is already registered by another user.", cause);
    }
}
