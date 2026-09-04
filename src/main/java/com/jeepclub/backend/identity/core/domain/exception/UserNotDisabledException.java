package com.jeepclub.backend.identity.core.domain.exception;

public class UserNotDisabledException extends IllegalStateException {

    public UserNotDisabledException(Long userId) {
        super("User is not disabled: " + userId);
    }
}
