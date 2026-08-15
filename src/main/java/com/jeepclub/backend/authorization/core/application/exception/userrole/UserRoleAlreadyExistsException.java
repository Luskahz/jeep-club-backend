package com.jeepclub.backend.authorization.core.application.exception.userrole;

public class UserRoleAlreadyExistsException extends RuntimeException {

    public UserRoleAlreadyExistsException(
            Long userId,
            Long roleId
    ) {
        super("Role already assigned to user. User id: "
                + userId
                + ", role id: "
                + roleId);
    }
}