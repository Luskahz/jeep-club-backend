package com.jeepclub.backend.dependents.core.domain.exception;

public class DependentAlreadyDeletedException extends RuntimeException {

    public DependentAlreadyDeletedException(Long dependentId) {
        super(
                "Dependent with id "
                        + dependentId
                        + " is already deleted."
        );
    }
}