package com.jeepclub.backend.authorization.core.domain.exception.role;

public class InactiveRoleCannotBeUsedException extends RuntimeException {

    public InactiveRoleCannotBeUsedException(Long roleId) {
        super("Inactive role cannot be used. Role id: " + roleId);
    }
}