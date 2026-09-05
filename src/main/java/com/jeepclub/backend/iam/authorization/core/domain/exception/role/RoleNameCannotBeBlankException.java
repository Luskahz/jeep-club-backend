package com.jeepclub.backend.iam.authorization.core.domain.exception.role;

public class RoleNameCannotBeBlankException extends RuntimeException {

    public RoleNameCannotBeBlankException() {
        super("Role name cannot be blank.");
    }
}