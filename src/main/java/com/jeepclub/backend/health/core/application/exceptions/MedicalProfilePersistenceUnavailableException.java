package com.jeepclub.backend.health.core.application.exceptions;

public class MedicalProfilePersistenceUnavailableException extends RuntimeException {

    public MedicalProfilePersistenceUnavailableException(Throwable cause) {
        super(
                "O armazenamento de perfis médicos está temporariamente indisponível.",
                cause
        );
    }
}
