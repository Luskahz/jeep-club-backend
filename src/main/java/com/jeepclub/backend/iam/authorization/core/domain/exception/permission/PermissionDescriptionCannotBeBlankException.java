package com.jeepclub.backend.iam.authorization.core.domain.exception.permission;

public class PermissionDescriptionCannotBeBlankException extends RuntimeException {

    public PermissionDescriptionCannotBeBlankException() {
        super("Permission description cannot be blank.");
    }
}