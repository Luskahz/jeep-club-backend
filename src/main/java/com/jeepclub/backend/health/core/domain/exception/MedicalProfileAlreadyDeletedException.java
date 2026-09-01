package com.jeepclub.backend.health.core.domain.exception;

public class MedicalProfileAlreadyDeletedException extends RuntimeException {

    public MedicalProfileAlreadyDeletedException(Long medicalProfileId) {
        super(
                "Medical profile with id "
                        + medicalProfileId
                        + " is already deleted."
        );
    }
}
