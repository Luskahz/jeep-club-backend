package com.jeepclub.backend.health.core.application.query;
import com.jeepclub.backend.health.api.module.medicalprofile.*;
import com.jeepclub.backend.health.core.repository.MedicalProfileRepository;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.port.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
@Service @RequiredArgsConstructor
public class EmergencyMedicalProfileQueryService implements EmergencyMedicalProfileQuery {
    private final MedicalProfileRepository profiles;
    private final MedicalProfileOwnerStatusChecker owners;
    @Transactional(readOnly=true)
    public Optional<Profile> find(MedicalProfileOwner type, Long id) {
        var owner = MedicalProfileOwnerType.valueOf(type.name());
        if (owners.getStatus(owner, id) != MedicalProfileOwnerStatus.ACTIVE) return Optional.empty();
        return profiles.findByOwner(owner, id).map(p -> new Profile(p.getBloodType().name(), p.getAllergies(), p.getChronicConditions(),
            p.getContinuousMedications(), p.getHealthInsuranceProvider(), p.getHealthInsurancePlan(), p.getHealthInsuranceNumber(),
            p.getEmergencyContactName(), p.getEmergencyContactPhone(), p.getEmergencyContactRelationship(), p.getObservations()));
    }
}
