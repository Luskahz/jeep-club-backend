package com.jeepclub.backend.medical.core.domain.exceptions;

public class InvalidMedicalProfileException extends RuntimeException {

    public InvalidMedicalProfileException(String message) {
        super(message);
    }
}
