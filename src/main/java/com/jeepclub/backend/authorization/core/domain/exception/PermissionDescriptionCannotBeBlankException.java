package com.jeepclub.backend.authorization.core.domain.exception;

public class PermissionDescriptionCannotBeBlankException extends RuntimeException {

    public PermissionDescriptionCannotBeBlankException() {
        super("Permission description cannot be blank.");
    }
}