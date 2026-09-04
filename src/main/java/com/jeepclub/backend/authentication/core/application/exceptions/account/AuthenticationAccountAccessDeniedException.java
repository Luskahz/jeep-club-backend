package com.jeepclub.backend.authentication.core.application.exceptions.account;

public class AuthenticationAccountAccessDeniedException extends RuntimeException {

    public AuthenticationAccountAccessDeniedException(String message) {
        super(message);
    }
}
