package com.jeepclub.backend.dependents.core.application.exception;

public class DependentOwnerInactiveException extends RuntimeException {

    public DependentOwnerInactiveException(Long userId) {
        super(
                "Dependent owner is inactive with user id: "
                        + userId
        );
    }
}
