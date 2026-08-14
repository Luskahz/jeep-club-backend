package com.jeepclub.backend.health.core.application.service.medicalprofile;

import com.jeepclub.backend.health.core.application.MedicalProfileData;
import com.jeepclub.backend.health.core.application.service.medicalprofile.internal.MedicalProfileManager;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminMedicalProfileService {

    private final MedicalProfileManager medicalProfileManager;

    @Transactional(readOnly = true)
    public MedicalProfile getById(Long id) {
        return medicalProfileManager.getById(id);
    }

    @Transactional(readOnly = true)
    public MedicalProfile getByOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    ) {
        return medicalProfileManager.getByOwner(ownerType, ownerId);
    }

    @Transactional(readOnly = true)
    public List<MedicalProfile> listMedicalProfiles(int page, int size) {
        return medicalProfileManager.listMedicalProfiles(page, size);
    }

    @Transactional
    public MedicalProfile upsertByOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId,
            MedicalProfileData data
    ) {
        return medicalProfileManager.upsertByOwner(ownerType, ownerId, data);
    }
}
