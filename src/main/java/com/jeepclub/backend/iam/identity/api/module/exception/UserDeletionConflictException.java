package com.jeepclub.backend.iam.identity.api.module.exception;
/** A retained relationship or completed authentication makes physical deletion unsafe. */
public class UserDeletionConflictException extends RuntimeException {
    public UserDeletionConflictException(String message) { super(message); }
}
