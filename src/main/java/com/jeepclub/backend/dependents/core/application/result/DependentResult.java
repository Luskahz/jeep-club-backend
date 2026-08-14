package com.jeepclub.backend.dependents.core.application.result;

import com.jeepclub.backend.dependents.core.domain.model.Dependent;
import com.jeepclub.backend.dependents.core.port.DependentMedicalProfileData;

public record DependentResult(
        Dependent dependent,
        DependentMedicalProfileData medicalProfile
) {
}
