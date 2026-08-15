package com.jeepclub.backend.authentication.infra.exception.user;

public class InvalidUserSortFieldException extends RuntimeException {

    public InvalidUserSortFieldException(String field) {
        super("Unsupported user sort field: " + field);
    }
}