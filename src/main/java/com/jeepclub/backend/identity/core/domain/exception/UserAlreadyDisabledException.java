package com.jeepclub.backend.identity.core.domain.exception;

public class UserAlreadyDisabledException extends IllegalStateException {

    public UserAlreadyDisabledException(Long userId) {
        super("User is already disabled: " + userId);
    }
}
