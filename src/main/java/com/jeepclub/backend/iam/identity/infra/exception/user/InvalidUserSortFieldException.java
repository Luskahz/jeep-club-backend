package com.jeepclub.backend.iam.identity.infra.exception.user;

public class InvalidUserSortFieldException extends IllegalArgumentException {
    public InvalidUserSortFieldException(String field) {
        super("Unsupported user sort field: " + field);
    }
}
