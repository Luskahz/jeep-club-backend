package com.jeepclub.backend.authorization.core.application.exception;

public class RootRoleCannotBeManagedManuallyException extends RuntimeException {

    public RootRoleCannotBeManagedManuallyException(Long roleId) {
        super(
                "ROOT role is system-managed and cannot be manually assigned or revoked. Role id: "
                        + roleId
        );
    }
}