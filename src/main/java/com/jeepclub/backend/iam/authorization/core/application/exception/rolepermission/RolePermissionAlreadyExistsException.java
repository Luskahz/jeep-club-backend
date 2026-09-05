package com.jeepclub.backend.iam.authorization.core.application.exception.rolepermission;

public class RolePermissionAlreadyExistsException extends RuntimeException {

    public RolePermissionAlreadyExistsException(
            Long roleId,
            Long permissionId
    ) {
        super("Permission already assigned to role. Role id: "
                + roleId
                + ", permission id: "
                + permissionId);
    }
}