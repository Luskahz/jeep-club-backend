package com.jeepclub.backend.health.core.application;

import com.jeepclub.backend.health.api.http.dto.MedicalProfileRequest;
import com.jeepclub.backend.health.core.application.exceptions.InvalidMedicalProfileDataException;
import com.jeepclub.backend.health.core.domain.BloodType;
import com.jeepclub.backend.health.core.domain.MedicalProfile;
import com.jeepclub.backend.health.core.domain.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.ports.DependentOwnershipChecker;
import com.jeepclub.backend.health.core.repository.MedicalProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MedicalProfileMutationCandidateTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-06-30T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    private MedicalProfileService service;

    @BeforeEach
    void setUp() {
        service = new MedicalProfileService(
                new SingleSlotRepository(),
                Optional.<DependentOwnershipChecker>empty(),
                FIXED_CLOCK
        );
    }

    @Test
    void deveMatarMutantesNasRegrasDeTamanhoDoTelefone() {
        assertEquals("1233334444", service.upsertMyMedicalProfile(10L, request("1233334444")).getEmergencyContactPhone());
        assertEquals("12999999999", service.upsertMyMedicalProfile(10L, request("12999999999")).getEmergencyContactPhone());

        assertThrows(InvalidMedicalProfileDataException.class, () -> service.upsertMyMedicalProfile(10L, request("123456789")));
        assertThrows(InvalidMedicalProfileDataException.class, () -> service.upsertMyMedicalProfile(10L, request("123456789012")));
    }

    @Test
    void deveMatarMutanteQueRemoveDefaultUnknownDoTipoSanguineo() {
        MedicalProfileRequest request = new MedicalProfileRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "12999999999",
                null,
                null
        );

        MedicalProfile profile = service.upsertMyMedicalProfile(10L, request);

        assertEquals(BloodType.UNKNOWN, profile.getBloodType());
    }

    private MedicalProfileRequest request(String phone) {
        return new MedicalProfileRequest(
                BloodType.O_POSITIVE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                phone,
                null,
                null
        );
    }

    private static class SingleSlotRepository implements MedicalProfileRepository {

        private MedicalProfile stored;

        @Override
        public Optional<MedicalProfile> findById(Long id) {
            return Optional.empty();
        }

        @Override
        public Optional<MedicalProfile> findByOwner(MedicalProfileOwnerType ownerType, Long ownerId) {
            return Optional.ofNullable(stored)
                    .filter(profile -> profile.getOwnerType() == ownerType && profile.getOwnerId().equals(ownerId));
        }

        @Override
        public List<MedicalProfile> findAll(int page, int size) {
            return stored == null ? List.of() : List.of(stored);
        }

        @Override
        public MedicalProfile save(MedicalProfile medicalProfile) {
            this.stored = medicalProfile;
            return medicalProfile;
        }
    }
}
