package com.jeepclub.backend.dependents.core.domain.exception;

public class DependentLifecycleConflictException extends RuntimeException {
    public DependentLifecycleConflictException(String message) { super(message); }
}
