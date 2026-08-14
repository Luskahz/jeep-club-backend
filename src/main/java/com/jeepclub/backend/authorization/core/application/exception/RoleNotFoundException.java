package com.jeepclub.backend.authorization.core.application.exception;

public class RoleNotFoundException extends RuntimeException {

    public RoleNotFoundException(Long roleId) {
        super("Role not found with id: " + roleId);
    }
}
