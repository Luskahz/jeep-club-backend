package com.jeepclub.backend.health.core.application.exceptions;

import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;

public class MedicalProfileConflictException extends RuntimeException {

    private MedicalProfileConflictException(String message, Throwable cause) {
        super(message, cause);
    }

    public static MedicalProfileConflictException ownerAlreadyHasProfile(
            MedicalProfileOwnerType ownerType,
            Long ownerId,
            Throwable cause
    ) {
        return new MedicalProfileConflictException(
                "Já existe um perfil médico para " + ownerType
                        + " com ID " + ownerId + ".",
                cause
        );
    }

    public static MedicalProfileConflictException concurrentUpdate(
            MedicalProfileOwnerType ownerType,
            Long ownerId,
            Throwable cause
    ) {
        return new MedicalProfileConflictException(
                "O perfil médico de " + ownerType + " com ID " + ownerId
                        + " está sendo alterado por outra requisição.",
                cause
        );
    }
}
