package com.jeepclub.backend.authorization.core.domain.exception.role;

public class RootRoleCannotBeChangedException extends RuntimeException {

    public RootRoleCannotBeChangedException(Long roleId) {
        super("ROOT role cannot be changed. Role id: " + roleId);
    }
}