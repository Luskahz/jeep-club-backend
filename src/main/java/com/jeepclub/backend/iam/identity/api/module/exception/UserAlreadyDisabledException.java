package com.jeepclub.backend.iam.identity.api.module.exception;

public class UserAlreadyDisabledException extends IllegalStateException {

    public UserAlreadyDisabledException(Long userId, Throwable cause) {
        super("User is already disabled: " + userId, cause);
    }
}
