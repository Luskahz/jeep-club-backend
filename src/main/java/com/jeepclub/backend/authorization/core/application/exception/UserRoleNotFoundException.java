package com.jeepclub.backend.authorization.core.application.exception;

public class UserRoleNotFoundException extends RuntimeException {

    public UserRoleNotFoundException(
            Long userId,
            Long roleId
    ) {
        super("Role is not assigned to user. User id: "
                + userId
                + ", role id: "
                + roleId);
    }
}