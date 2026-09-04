package com.jeepclub.backend.iam.authentication.core.application.exceptions.login;

public class PasswordChangeNotRequiredException extends RuntimeException {

    public PasswordChangeNotRequiredException() {
        super("Authentication account does not have a pending password change.");
    }
}
