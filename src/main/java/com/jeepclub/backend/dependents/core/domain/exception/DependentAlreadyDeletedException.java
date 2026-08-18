package com.jeepclub.backend.dependents.core.domain.exception;

public class DependentAlreadyDeletedException extends DependentException {

    public DependentAlreadyDeletedException(Long dependentId) {
        super("Dependent " + dependentId + " is already deleted.");
    }
}