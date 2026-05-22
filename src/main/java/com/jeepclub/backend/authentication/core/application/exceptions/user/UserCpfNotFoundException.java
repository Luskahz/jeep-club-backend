package com.jeepclub.backend.authentication.core.application.exceptions.user;

public class UserCpfNotFoundException extends RuntimeException {

    public UserCpfNotFoundException(String message) {
        super(message);
    }
}
