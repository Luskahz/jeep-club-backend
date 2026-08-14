package com.jeepclub.backend.health.core.application.service.medicalprofile.internal;

import com.jeepclub.backend.health.core.application.MedicalProfileData;
import com.jeepclub.backend.health.core.application.exceptions.InvalidMedicalProfileDataException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileNotFoundException;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import com.jeepclub.backend.health.core.repository.MedicalProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MedicalProfileManager {

    private static final int MAX_PAGE_SIZE = 100;

    private final MedicalProfileRepository medicalProfileRepository;
    private final Clock clock;

    public MedicalProfile getById(Long id) {
        if (id == null) {
            throw new InvalidMedicalProfileDataException("O ID do perfil médico é obrigatório.");
        }

        return medicalProfileRepository
                .findById(id)
                .orElseThrow(MedicalProfileNotFoundException::new);
    }

    public MedicalProfile getByOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    ) {
        validateOwner(ownerType, ownerId);

        return medicalProfileRepository
                .findByOwner(ownerType, ownerId)
                .orElseThrow(MedicalProfileNotFoundException::new);
    }

    public List<MedicalProfile> listMedicalProfiles(int page, int size) {
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = sanitizePageSize(size);

        return medicalProfileRepository.findAll(sanitizedPage, sanitizedSize);
    }

    public MedicalProfile upsertByOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId,
            MedicalProfileData data
    ) {
        validateOwner(ownerType, ownerId);

        return medicalProfileRepository
                .findByOwner(ownerType, ownerId)
                .map(existing -> updateExisting(existing, data))
                .orElseGet(() -> createNew(ownerType, ownerId, data));
    }

    private MedicalProfile updateExisting(
            MedicalProfile existing,
            MedicalProfileData data
    ) {
        Instant now = Instant.now(clock);
        existing.update(
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
                now
        );

        return medicalProfileRepository.save(existing);
    }

    private MedicalProfile createNew(
            MedicalProfileOwnerType ownerType,
            Long ownerId,
            MedicalProfileData data
    ) {
        Instant now = Instant.now(clock);

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
                now
        );

        return medicalProfileRepository.save(profile);
    }

    private void validateOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    ) {
        if (ownerType == null) {
            throw new InvalidMedicalProfileDataException("O tipo do proprietário do perfil médico é obrigatório.");
        }

        if (ownerId == null) {
            throw new InvalidMedicalProfileDataException("O ID do proprietário do perfil médico é obrigatório.");
        }
    }

    private int sanitizePageSize(int size) {
        if (size <= 0) {
            return 20;
        }

        return Math.min(size, MAX_PAGE_SIZE);
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
