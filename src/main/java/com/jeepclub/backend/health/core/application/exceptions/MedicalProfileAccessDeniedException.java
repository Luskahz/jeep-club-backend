package com.jeepclub.backend.health.core.application.exceptions;

public class MedicalProfileAccessDeniedException extends RuntimeException {

    public MedicalProfileAccessDeniedException(String message) {
        super(message);
    }
}
