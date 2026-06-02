package com.jeepclub.backend.health.core.domain.exceptions;

public class InvalidMedicalProfileException extends RuntimeException {

    public InvalidMedicalProfileException(String message) {
        super(message);
    }
}
