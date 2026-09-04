package com.jeepclub.backend.identity.api.module.exception;

public class UserNotDisabledException extends IllegalStateException {

    public UserNotDisabledException(Long userId, Throwable cause) {
        super("User is not disabled: " + userId, cause);
    }
}
