package com.jeepclub.backend.medical.core.application;

import com.jeepclub.backend.medical.api.dto.MedicalProfileRequest;
import com.jeepclub.backend.medical.core.domain.MedicalProfile;
import com.jeepclub.backend.medical.core.domain.MedicalProfileOwnerType;
import com.jeepclub.backend.medical.core.repository.MedicalProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
// service ok, porem não deixe as exceptions pra depois, faça as exceptions personalizadas. crie o handler.
// separe em exceptions em application.exceptions para exceptions que explodem no service. e domain.exceptions para exceptions que
// explodem no model com a defesa da classe.


// se sentir que está retornando muita coisa pro controller em um metodo só, crie results
// eles ficam em application.results.arquivo.java ao lado de application.service.arquivo.java
public class MedicalProfileService {

    private final MedicalProfileRepository medicalProfileRepository;

    @Transactional(readOnly = true)
    public MedicalProfile getByOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    ) {
        return medicalProfileRepository
                .findByOwner(ownerType, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Perfil médico não encontrado."));
    }

    @Transactional
    public MedicalProfile upsert(
            MedicalProfileOwnerType ownerType,
            Long ownerId,
            MedicalProfileRequest request
    ) {
        return medicalProfileRepository
                .findByOwner(ownerType, ownerId)
                .map(existing -> updateExisting(existing, request))
                .orElseGet(() -> createNew(ownerType, ownerId, request));
    }

    private MedicalProfile updateExisting(
            MedicalProfile existing,
            MedicalProfileRequest request
    ) {
        existing.update(
                request.bloodType(),
                clean(request.allergies()),
                clean(request.chronicConditions()),
                clean(request.continuousMedications()),
                clean(request.healthInsuranceProvider()),
                clean(request.healthInsurancePlan()),
                clean(request.healthInsuranceNumber()),
                clean(request.emergencyContactName()),
                normalizePhone(request.emergencyContactPhone()),
                clean(request.emergencyContactRelationship()),
                clean(request.observations())
        );

        return medicalProfileRepository.save(existing);
    }


    // aqui cabe usar um construtor personalizado, estude sobre os construtores create e reconstitute, n cheguei na sua classe aqui
    // mas sinto que ela pode estar fraca.
    private MedicalProfile createNew(
            MedicalProfileOwnerType ownerType,
            Long ownerId,
            MedicalProfileRequest request
    ) {
        var profile = new MedicalProfile(
                null,
                ownerType,
                ownerId,
                request.bloodType(),
                clean(request.allergies()),
                clean(request.chronicConditions()),
                clean(request.continuousMedications()),
                clean(request.healthInsuranceProvider()),
                clean(request.healthInsurancePlan()),
                clean(request.healthInsuranceNumber()),
                clean(request.emergencyContactName()),
                normalizePhone(request.emergencyContactPhone()),
                clean(request.emergencyContactRelationship()),
                clean(request.observations()),
                Instant.now(),
                Instant.now()
        );

        return medicalProfileRepository.save(profile);
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
            throw new IllegalArgumentException("O telefone de emergência deve ter 10 ou 11 dígitos.");
        }

        return digits;
    }
}