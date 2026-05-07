package com.jeepclub.backend.authorization.core.application.exception;

public class RoleAlreadyExistsException extends RuntimeException {

    public RoleAlreadyExistsException(String name) {
        super("Role already exists with name: " + name);
    }
}