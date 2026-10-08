package com.jeepclub.backend.health.infra.persistence;

import com.jeepclub.backend.health.core.domain.enums.BloodType;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.infra.persistence.entity.MedicalProfileEntity;
import com.jeepclub.backend.health.infra.persistence.jpa.MedicalProfileJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MedicalProfileJpaIntegrationTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-06-30T12:00:00Z");

    @Autowired
    private MedicalProfileJpaRepository repository;

    @Autowired
    private com.jeepclub.backend.health.core.repository.MedicalProfileCoverageRepository coverage;

    @Test
    void sameNumericOwnerIdIsIsolatedByTypeIncludingCoverageProjection() {
        repository.saveAndFlush(entity(MedicalProfileOwnerType.USER, 71001L, BloodType.UNKNOWN, null));
        repository.saveAndFlush(entity(MedicalProfileOwnerType.DEPENDENT, 71001L, BloodType.UNKNOWN, null));
        repository.saveAndFlush(entity(MedicalProfileOwnerType.DEPENDENT, 71002L, BloodType.UNKNOWN, null));
        var user = repository.findByOwnerTypeAndOwnerId(MedicalProfileOwnerType.USER, 71001L).orElseThrow();
        var dependent = repository.findByOwnerTypeAndOwnerId(MedicalProfileOwnerType.DEPENDENT, 71001L).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(user.getId()).isNotEqualTo(dependent.getId());
        var ids = java.util.List.of(71001L, 71002L);
        org.assertj.core.api.Assertions.assertThat(coverage.findCovered(
                com.jeepclub.backend.health.api.module.medicalprofile.MedicalProfileOwner.USER, ids)).containsExactly(71001L);
        org.assertj.core.api.Assertions.assertThat(coverage.findCovered(
                com.jeepclub.backend.health.api.module.medicalprofile.MedicalProfileOwner.DEPENDENT, java.util.List.of(71001L))).containsExactly(71001L);
    }

    @Test
    void deveSalvarEBuscarPerfilMedicoPorOwnerTypeEOwnerId() {
        MedicalProfileEntity entity = entity(
                MedicalProfileOwnerType.USER,
                10L,
                BloodType.O_POSITIVE,
                "Dipirona"
        );

        repository.saveAndFlush(entity);

        var result = repository.findByOwnerTypeAndOwnerId(
                MedicalProfileOwnerType.USER,
                10L
        );

        assertTrue(result.isPresent());
        assertEquals(BloodType.O_POSITIVE, result.get().getBloodType());
        assertEquals("Dipirona", result.get().getAllergies());
    }

    @Test
    void deveImpedirDoisPerfisMedicosParaOMesmoOwner() {
        repository.saveAndFlush(entity(
                MedicalProfileOwnerType.USER,
                10L,
                BloodType.O_POSITIVE,
                "Dipirona"
        ));

        MedicalProfileEntity duplicate = entity(
                MedicalProfileOwnerType.USER,
                10L,
                BloodType.A_POSITIVE,
                "Amendoim"
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(duplicate)
        );
    }

    private MedicalProfileEntity entity(
            MedicalProfileOwnerType ownerType,
            Long ownerId,
            BloodType bloodType,
            String allergies
    ) {
        MedicalProfileEntity entity = new MedicalProfileEntity();
        entity.setOwnerType(ownerType);
        entity.setOwnerId(ownerId);
        entity.setBloodType(bloodType);
        entity.setAllergies(allergies);
        entity.setChronicConditions("Asma");
        entity.setContinuousMedications("Bombinha");
        entity.setHealthInsuranceProvider("Unimed");
        entity.setHealthInsurancePlan("Enfermaria");
        entity.setHealthInsuranceNumber("123456789");
        entity.setEmergencyContactName("Maria");
        entity.setEmergencyContactPhone("12999999999");
        entity.setEmergencyContactRelationship("Mãe");
        entity.setObservations("Observação");
        entity.setCreatedAt(FIXED_NOW);
        entity.setUpdatedAt(FIXED_NOW);
        return entity;
    }
}
