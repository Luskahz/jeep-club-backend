package com.jeepclub.backend.dependents.core.application.exception;

public class DependentAccessDeniedException extends RuntimeException {

    public DependentAccessDeniedException(Long dependentId) {
        super(
                "Access denied to dependent with id: "
                        + dependentId
        );
    }
}
