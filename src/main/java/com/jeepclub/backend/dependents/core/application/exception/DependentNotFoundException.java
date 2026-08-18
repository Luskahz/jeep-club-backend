package com.jeepclub.backend.dependents.core.application.exception;


public class DependentNotFoundException extends RuntimeException {

    public DependentNotFoundException(Long id) {
        super("Dependent not found: " + id);
    }
}
