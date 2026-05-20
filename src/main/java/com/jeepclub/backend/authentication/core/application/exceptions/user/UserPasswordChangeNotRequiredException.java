package com.jeepclub.backend.authentication.core.application.exceptions.user;

public class UserPasswordChangeNotRequiredException extends RuntimeException {

    public UserPasswordChangeNotRequiredException() {
        super("User does not have a pending password change.");
    }
}