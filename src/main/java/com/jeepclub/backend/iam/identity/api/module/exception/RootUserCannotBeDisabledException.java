package com.jeepclub.backend.iam.identity.api.module.exception;

public class RootUserCannotBeDisabledException extends IllegalStateException {

    public RootUserCannotBeDisabledException(Long userId) {
        super("User with ROOT role cannot be disabled: " + userId);
    }
}
