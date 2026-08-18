package com.jeepclub.backend.dependents.core.application.exception;

public class DependentAlreadyDeletedException extends RuntimeException {

    public DependentAlreadyDeletedException(Long id) {
        super("Dependent is already deleted: " + id);
    }
}