package com.jeepclub.backend.health.core.application.service.medicalprofile;

import com.jeepclub.backend.health.core.application.command.UpsertMedicalProfileCommand;
import com.jeepclub.backend.health.core.application.exceptions.InvalidMedicalProfileDataException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileAccessDeniedException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileNotFoundException;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import com.jeepclub.backend.health.core.port.DependentOwnershipChecker;
import com.jeepclub.backend.health.core.repository.MedicalProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MedicalProfileService {

    private final MedicalProfileRepository medicalProfileRepository;
    private final DependentOwnershipChecker dependentOwnershipChecker;
    private final Clock clock;

    @Transactional(readOnly = true)
    public MedicalProfile getMyMedicalProfile(Long userId) {
        return findByOwner(
                MedicalProfileOwnerType.USER,
                userId
        );
    }

    @Transactional
    public MedicalProfile upsertMyMedicalProfile(
            Long userId,
            UpsertMedicalProfileCommand data
    ) {
        return upsertByOwner(
                MedicalProfileOwnerType.USER,
                userId,
                data
        );
    }

    @Transactional(readOnly = true)
    public MedicalProfile getDependentMedicalProfile(
            Long userId,
            Long dependentId
    ) {
        validateDependentBelongsToUser(
                dependentId,
                userId
        );

        return findByOwner(
                MedicalProfileOwnerType.DEPENDENT,
                dependentId
        );
    }

    @Transactional
    public MedicalProfile upsertDependentMedicalProfile(
            Long userId,
            Long dependentId,
            UpsertMedicalProfileCommand data
    ) {
        validateDependentBelongsToUser(
                dependentId,
                userId
        );

        return upsertByOwner(
                MedicalProfileOwnerType.DEPENDENT,
                dependentId,
                data
        );
    }

    private MedicalProfile findByOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    ) {
        return medicalProfileRepository
                .findByOwner(ownerType, ownerId)
                .orElseThrow(MedicalProfileNotFoundException::new);
    }

    private MedicalProfile upsertByOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId,
            UpsertMedicalProfileCommand data
    ) {
        return medicalProfileRepository
                .findByOwner(ownerType, ownerId)
                .map(profile -> updateProfile(profile, data))
                .orElseGet(() -> createProfile(
                        ownerType,
                        ownerId,
                        data
                ));
    }

    private MedicalProfile updateProfile(
            MedicalProfile profile,
            UpsertMedicalProfileCommand data
    ) {
        profile.update(
                data.bloodType(),
                clean(data.allergies()),
                clean(data.chronicConditions()),
                clean(data.continuousMedications()),
                clean(data.healthInsuranceProvider()),
                clean(data.healthInsurancePlan()),
                clean(data.healthInsuranceNumber()),
                clean(data.emergencyContactName()),
                normalizePhone(data.emergencyContactPhone()),
                clean(data.emergencyContactRelationship()),
                clean(data.observations()),
                Instant.now(clock)
        );

        return medicalProfileRepository.save(profile);
    }

    private MedicalProfile createProfile(
            MedicalProfileOwnerType ownerType,
            Long ownerId,
            UpsertMedicalProfileCommand data
    ) {
        MedicalProfile profile = MedicalProfile.create(
                ownerType,
                ownerId,
                data.bloodType(),
                clean(data.allergies()),
                clean(data.chronicConditions()),
                clean(data.continuousMedications()),
                clean(data.healthInsuranceProvider()),
                clean(data.healthInsurancePlan()),
                clean(data.healthInsuranceNumber()),
                clean(data.emergencyContactName()),
                normalizePhone(data.emergencyContactPhone()),
                clean(data.emergencyContactRelationship()),
                clean(data.observations()),
                Instant.now(clock)
        );

        return medicalProfileRepository.save(profile);
    }

    private void validateDependentBelongsToUser(
            Long dependentId,
            Long userId
    ) {
        if (dependentId == null) {
            throw new InvalidMedicalProfileDataException(
                    "O ID do dependente é obrigatório."
            );
        }

        if (userId == null) {
            throw new InvalidMedicalProfileDataException(
                    "O ID do usuário autenticado é obrigatório."
            );
        }

        if (!dependentOwnershipChecker.belongsToUser(
                dependentId,
                userId
        )) {
            throw new MedicalProfileAccessDeniedException(
                    "O dependente informado não pertence ao usuário autenticado."
            );
        }
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }

        String digits = phone.replaceAll("\\D", "");

        if (digits.isBlank()) {
            return null;
        }

        if (digits.length() < 10 || digits.length() > 11) {
            throw new InvalidMedicalProfileDataException(
                    "O telefone de emergência deve ter 10 ou 11 dígitos."
            );
        }

        return digits;
    }
}