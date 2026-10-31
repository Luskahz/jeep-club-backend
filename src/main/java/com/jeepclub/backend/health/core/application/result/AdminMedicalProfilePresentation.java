package com.jeepclub.backend.health.core.application.result;

import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import com.jeepclub.backend.health.core.port.MedicalProfileOwnerPresentationQuery.Names;

import java.util.Objects;

/** Request-scoped read projection, never persisted or cached. */
public record AdminMedicalProfilePresentation(MedicalProfile profile, Names names) {
    public AdminMedicalProfilePresentation {
        Objects.requireNonNull(profile);
        Objects.requireNonNull(names);
    }

    @Override public String toString() { return "AdminMedicalProfilePresentation[REDACTED]"; }
}
