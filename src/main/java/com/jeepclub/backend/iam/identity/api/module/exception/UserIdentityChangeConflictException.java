package com.jeepclub.backend.iam.identity.api.module.exception;

/** Authentication cannot apply the required atomic effects for a contact correction. */
public class UserIdentityChangeConflictException extends RuntimeException {
    public UserIdentityChangeConflictException() { super("Authentication account required for identity correction does not exist."); }
}
