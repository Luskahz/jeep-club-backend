package com.jeepclub.backend.iam.authorization.core.domain.exception.role;

public class RoleDescriptionTooLongException extends RuntimeException {

    public RoleDescriptionTooLongException(int maxLength) {
        super("Role description cannot exceed " + maxLength + " characters.");
    }
}