package com.jeepclub.backend.dependents.core.domain.exception;

public class DependentAccessDeniedException extends RuntimeException {
    public DependentAccessDeniedException(String message) {
        super(message);
    }
}
