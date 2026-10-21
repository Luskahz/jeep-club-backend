package com.jeepclub.backend.tools.core.domain.exception;

public class ToolLifecycleConflictException extends RuntimeException {
    public ToolLifecycleConflictException(String message) { super(message); }
}
