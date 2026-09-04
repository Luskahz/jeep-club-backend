package com.jeepclub.backend.identity.api.module.exception;

public class UserAlreadyDisabledException extends IllegalStateException {

    public UserAlreadyDisabledException(Long identityId, Throwable cause) {
        super("User is already disabled: " + identityId, cause);
    }
}
