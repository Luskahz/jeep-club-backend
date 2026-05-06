package com.jeepclub.backend.authorization.core.application.exception;

public class RolePermissionNotFoundException extends RuntimeException {

    public RolePermissionNotFoundException(
            Long roleId,
            Long permissionId
    ) {
        super("Permission is not assigned to role. Role id: "
                + roleId
                + ", permission id: "
                + permissionId);
    }
}