package com.jeepclub.backend.health.core.application.query;

import com.jeepclub.backend.health.api.module.medicalprofile.MedicalProfileOwner;
import com.jeepclub.backend.health.api.module.medicalprofile.MedicalProfileQuery;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.repository.MedicalProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class HealthMedicalProfileQueryService implements MedicalProfileQuery {

    private final MedicalProfileRepository medicalProfileRepository;

    @Override
    public boolean existsByOwner(
            MedicalProfileOwner owner,
            Long ownerId
    ) {
        if (owner == null || ownerId == null) {
            return false;
        }

        return medicalProfileRepository.existsByOwner(
                toDomain(owner),
                ownerId
        );
    }

    private MedicalProfileOwnerType toDomain(
            MedicalProfileOwner owner
    ) {
        return MedicalProfileOwnerType.valueOf(
                owner.name()
        );
    }
}