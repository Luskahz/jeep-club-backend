package com.jeepclub.backend.health.core.application.exceptions;

import com.jeepclub.backend.health.core.domain.exception.InvalidMedicalProfileException;

public class InvalidMedicalProfileDataException extends InvalidMedicalProfileException {

    public InvalidMedicalProfileDataException(String message) {
        super(message);
    }
}
