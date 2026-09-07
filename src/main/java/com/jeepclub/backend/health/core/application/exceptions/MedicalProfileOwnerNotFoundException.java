package com.jeepclub.backend.health.core.application.exceptions;

import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;

public class MedicalProfileOwnerNotFoundException extends RuntimeException {

    public MedicalProfileOwnerNotFoundException(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    ) {
        super("Proprietário do perfil médico não encontrado: "
                + ownerType + " com ID " + ownerId + ".");
    }
}
