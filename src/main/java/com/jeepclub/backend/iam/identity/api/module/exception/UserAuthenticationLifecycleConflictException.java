package com.jeepclub.backend.iam.identity.api.module.exception;

/** Invalid composite lifecycle, without exposing Authentication internals across modules. */
public class UserAuthenticationLifecycleConflictException extends RuntimeException {
    public UserAuthenticationLifecycleConflictException() {
        super("Authentication account required by the User lifecycle does not exist.");
    }
}
