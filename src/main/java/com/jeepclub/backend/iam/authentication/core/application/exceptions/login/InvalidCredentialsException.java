package com.jeepclub.backend.iam.authentication.core.application.exceptions.login;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid CPF or password.");
    }
}