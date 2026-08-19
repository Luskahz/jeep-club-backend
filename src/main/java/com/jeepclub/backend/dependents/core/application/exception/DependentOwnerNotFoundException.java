package com.jeepclub.backend.dependents.core.application.exception;

public class DependentOwnerNotFoundException extends RuntimeException {

    public DependentOwnerNotFoundException(Long userId) {
        super(
                "Dependent owner not found with user id: "
                        + userId
        );
    }
}

