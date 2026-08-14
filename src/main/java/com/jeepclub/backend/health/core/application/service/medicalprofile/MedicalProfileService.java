package com.jeepclub.backend.health.core.application.service.medicalprofile;

import com.jeepclub.backend.health.core.application.MedicalProfileData;
import com.jeepclub.backend.health.core.application.exceptions.DependentOwnershipValidationUnavailableException;
import com.jeepclub.backend.health.core.application.exceptions.InvalidMedicalProfileDataException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileAccessDeniedException;
import com.jeepclub.backend.health.core.application.service.medicalprofile.internal.MedicalProfileManager;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import com.jeepclub.backend.health.core.port.DependentOwnershipChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MedicalProfileService {

    private final MedicalProfileManager medicalProfileManager;
    private final Optional<DependentOwnershipChecker> dependentOwnershipChecker;

    @Transactional(readOnly = true)
    public MedicalProfile getMyMedicalProfile(Long userId) {
        return medicalProfileManager.getByOwner(MedicalProfileOwnerType.USER, userId);
    }

    @Transactional
    public MedicalProfile upsertMyMedicalProfile(
            Long userId,
            MedicalProfileData data
    ) {
        return medicalProfileManager.upsertByOwner(MedicalProfileOwnerType.USER, userId, data);
    }

    @Transactional(readOnly = true)
    public MedicalProfile getDependentMedicalProfile(
            Long userId,
            Long dependentId
    ) {
        validateDependentBelongsToUser(dependentId, userId);
        return medicalProfileManager.getByOwner(MedicalProfileOwnerType.DEPENDENT, dependentId);
    }

    @Transactional
    public MedicalProfile upsertDependentMedicalProfile(
            Long userId,
            Long dependentId,
            MedicalProfileData data
    ) {
        validateDependentBelongsToUser(dependentId, userId);
        return medicalProfileManager.upsertByOwner(MedicalProfileOwnerType.DEPENDENT, dependentId, data);
    }

    private void validateDependentBelongsToUser(
            Long dependentId,
            Long userId
    ) {
        if (dependentId == null) {
            throw new InvalidMedicalProfileDataException("O ID do dependente é obrigatório.");
        }

        if (userId == null) {
            throw new InvalidMedicalProfileDataException("O ID do usuário autenticado é obrigatório.");
        }

        DependentOwnershipChecker checker = dependentOwnershipChecker
                .orElseThrow(DependentOwnershipValidationUnavailableException::new);

        if (!checker.belongsToUser(dependentId, userId)) {
            throw new MedicalProfileAccessDeniedException(
                    "O dependente informado não pertence ao usuário autenticado."
            );
        }
    }
}
