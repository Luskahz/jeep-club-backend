package com.jeepclub.backend.health.core.application.exceptions;

public class InvalidMedicalProfileDataException extends RuntimeException {

    public InvalidMedicalProfileDataException(String message) {
        super(message);
    }
}
