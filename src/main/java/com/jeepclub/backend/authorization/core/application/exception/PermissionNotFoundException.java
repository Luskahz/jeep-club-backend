package com.jeepclub.backend.authorization.core.application.exception;

public class PermissionNotFoundException extends RuntimeException {

    public PermissionNotFoundException(Long permissionId) {
        super("Permission not found with id: " + permissionId);
    }
    public PermissionNotFoundException(String permissionRawCode) {
        super("Permission not found with code: " + permissionRawCode);
    }
}