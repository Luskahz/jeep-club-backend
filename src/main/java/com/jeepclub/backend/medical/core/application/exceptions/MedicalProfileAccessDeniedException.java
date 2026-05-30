package com.jeepclub.backend.medical.core.application.exceptions;

public class MedicalProfileAccessDeniedException extends RuntimeException {

    public MedicalProfileAccessDeniedException(String message) {
        super(message);
    }
}
