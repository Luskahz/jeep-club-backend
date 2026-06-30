package com.jeepclub.backend.health.core.domain;

import com.jeepclub.backend.health.core.domain.exceptions.InvalidMedicalProfileException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedicalProfileDomainUnitTest {

    private static final Instant CREATED_AT = Instant.parse("2026-06-30T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-06-30T13:00:00Z");

    @Test
    void createQuandoBloodTypeForNuloDeveUsarUnknown() {
        MedicalProfile profile = MedicalProfile.create(
                MedicalProfileOwnerType.USER,
                10L,
                null,
                "Dipirona",
                "Asma",
                "Bombinha",
                "Unimed",
                "Enfermaria",
                "123456789",
                "Maria",
                "12999999999",
                "Mãe",
                "Observação médica",
                CREATED_AT
        );

        assertEquals(BloodType.UNKNOWN, profile.getBloodType());
        assertEquals(MedicalProfileOwnerType.USER, profile.getOwnerType());
        assertEquals(10L, profile.getOwnerId());
        assertNotNull(profile.getCreatedAt());
        assertNotNull(profile.getUpdatedAt());
        assertEquals(CREATED_AT, profile.getCreatedAt());
        assertEquals(CREATED_AT, profile.getUpdatedAt());
    }

    @Test
    void createQuandoOwnerTypeForNuloDeveLancarExcecaoDeDominio() {
        assertThrows(InvalidMedicalProfileException.class, () -> MedicalProfile.create(
                null,
                10L,
                BloodType.O_POSITIVE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                CREATED_AT
        ));
    }

    @Test
    void reconstituteQuandoIdForNuloDeveLancarExcecaoDeDominio() {
        assertThrows(InvalidMedicalProfileException.class, () -> MedicalProfile.reconstitute(
                null,
                MedicalProfileOwnerType.USER,
                10L,
                BloodType.O_POSITIVE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                CREATED_AT,
                UPDATED_AT
        ));
    }

    @Test
    void updateDeveAlterarDadosEAtualizarUpdatedAt() {
        MedicalProfile profile = MedicalProfile.create(
                MedicalProfileOwnerType.USER,
                10L,
                BloodType.UNKNOWN,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                CREATED_AT
        );

        Instant updatedAtBeforeUpdate = profile.getUpdatedAt();

        profile.update(
                BloodType.A_POSITIVE,
                "Amendoim",
                "Hipertensão",
                "Losartana",
                "Unimed",
                "Apartamento",
                "987654321",
                "João",
                "12988887777",
                "Pai",
                "Evitar esforço intenso",
                UPDATED_AT
        );

        assertEquals(BloodType.A_POSITIVE, profile.getBloodType());
        assertEquals("Amendoim", profile.getAllergies());
        assertEquals("Hipertensão", profile.getChronicConditions());
        assertEquals("Losartana", profile.getContinuousMedications());
        assertTrue(!profile.getUpdatedAt().isBefore(updatedAtBeforeUpdate));
        assertEquals(UPDATED_AT, profile.getUpdatedAt());
    }
}
