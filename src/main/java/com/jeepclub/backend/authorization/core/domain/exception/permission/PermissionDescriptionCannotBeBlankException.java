package com.jeepclub.backend.authorization.core.domain.exception.permission;

public class PermissionDescriptionCannotBeBlankException extends RuntimeException {

    public PermissionDescriptionCannotBeBlankException() {
        super("Permission description cannot be blank.");
    }
}