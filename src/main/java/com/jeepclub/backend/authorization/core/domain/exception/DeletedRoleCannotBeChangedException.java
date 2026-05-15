package com.jeepclub.backend.authorization.core.domain.exception;

public class DeletedRoleCannotBeChangedException extends RuntimeException {

    public DeletedRoleCannotBeChangedException(Long roleId) {
        super("Deleted role cannot be changed. Role id: " + roleId);
    }
}