package com.jeepclub.backend.iam.authorization.core.application.exception.role;

public class RoleNotFoundException extends RuntimeException {

    public RoleNotFoundException(Long roleId) {
        super("Role not found with id: " + roleId);
    }
}
