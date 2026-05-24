package com.jeepclub.backend.medical.core.application.exceptions;

public class InvalidMedicalProfileDataException extends RuntimeException {

    public InvalidMedicalProfileDataException(String message) {
        super(message);
    }
}
