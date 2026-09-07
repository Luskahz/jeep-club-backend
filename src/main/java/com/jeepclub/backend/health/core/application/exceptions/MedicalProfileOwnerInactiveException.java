package com.jeepclub.backend.health.core.application.exceptions;

import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;

public class MedicalProfileOwnerInactiveException extends RuntimeException {

    public MedicalProfileOwnerInactiveException(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    ) {
        super("O proprietário do perfil médico está inativo: "
                + ownerType + " com ID " + ownerId + ".");
    }
}
