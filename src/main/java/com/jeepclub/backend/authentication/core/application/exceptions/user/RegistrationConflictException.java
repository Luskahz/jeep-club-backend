package com.jeepclub.backend.authentication.core.application.exceptions.user;

public class RegistrationConflictException extends RuntimeException {

    public RegistrationConflictException() {
        super("Registration could not be completed with the provided data.");
    }
}
