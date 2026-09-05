package com.jeepclub.backend.iam.authorization.core.domain.exception.role;

public class RoleNameTooLongException extends RuntimeException {

    public RoleNameTooLongException(int maxLength) {
        super("Role name cannot exceed " + maxLength + " characters.");
    }
}