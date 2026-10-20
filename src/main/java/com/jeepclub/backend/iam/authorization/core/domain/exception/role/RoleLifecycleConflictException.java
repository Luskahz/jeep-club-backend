package com.jeepclub.backend.iam.authorization.core.domain.exception.role;

public class RoleLifecycleConflictException extends RuntimeException {
    public RoleLifecycleConflictException(String message) { super(message); }
    public RoleLifecycleConflictException(String message, Throwable cause) { super(message, cause); }
}
