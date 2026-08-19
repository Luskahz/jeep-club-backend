package com.jeepclub.backend.dependents.core.application.exception;

public class DependentNotFoundException extends RuntimeException {

    public DependentNotFoundException(Long dependentId) {
        super(
                "Dependent not found with id: "
                        + dependentId
        );
    }
}