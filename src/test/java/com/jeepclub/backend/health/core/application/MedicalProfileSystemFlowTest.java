package com.jeepclub.backend.health.core.application;

import com.jeepclub.backend.health.api.http.dto.MedicalProfileRequest;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileAccessDeniedException;
import com.jeepclub.backend.health.core.application.service.medicalprofile.MedicalProfileService;
import com.jeepclub.backend.health.core.application.service.medicalprofile.internal.MedicalProfileManager;
import com.jeepclub.backend.health.core.domain.enums.BloodType;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import com.jeepclub.backend.health.core.port.DependentOwnershipChecker;
import com.jeepclub.backend.health.core.repository.MedicalProfileRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MedicalProfileSystemFlowTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-06-30T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    @Test
    void deveExecutarFluxoCompletoDoModuloParaUsuarioEDependente() {
        InMemoryMedicalProfileRepository repository = new InMemoryMedicalProfileRepository();
        DependentOwnershipChecker ownershipChecker = (dependentId, userId) ->
                userId.equals(10L) && dependentId.equals(300L);

        MedicalProfileService service = new MedicalProfileService(
                new MedicalProfileManager(repository, FIXED_CLOCK),
                Optional.of(ownershipChecker)
        );

        MedicalProfile userProfile = service.upsertMyMedicalProfile(
                10L,
                request(BloodType.O_POSITIVE, "Dipirona", "(12) 99999-9999").toApplicationData()
        );

        MedicalProfile loadedUserProfile = service.getMyMedicalProfile(10L);

        assertEquals(MedicalProfileOwnerType.USER, userProfile.getOwnerType());
        assertEquals(10L, loadedUserProfile.getOwnerId());
        assertEquals("12999999999", loadedUserProfile.getEmergencyContactPhone());

        MedicalProfile dependentProfile = service.upsertDependentMedicalProfile(
                10L,
                300L,
                request(BloodType.A_POSITIVE, "Amendoim", "(12) 98888-7777").toApplicationData()
        );

        MedicalProfile loadedDependentProfile = service.getDependentMedicalProfile(10L, 300L);

        assertEquals(MedicalProfileOwnerType.DEPENDENT, dependentProfile.getOwnerType());
        assertEquals(300L, loadedDependentProfile.getOwnerId());
        assertEquals("Amendoim", loadedDependentProfile.getAllergies());

        assertThrows(
                MedicalProfileAccessDeniedException.class,
                () -> service.upsertDependentMedicalProfile(
                        10L,
                        999L,
                        request(BloodType.B_POSITIVE, "Látex", "(12) 97777-6666").toApplicationData()
                )
        );
    }

    private MedicalProfileRequest request(
            BloodType bloodType,
            String allergies,
            String phone
    ) {
        return new MedicalProfileRequest(
                bloodType,
                allergies,
                "Asma",
                "Bombinha",
                "Unimed",
                "Enfermaria",
                "123456789",
                "Maria",
                phone,
                "Mãe",
                "Observação"
        );
    }

    private static class InMemoryMedicalProfileRepository implements MedicalProfileRepository {

        private final Map<String, MedicalProfile> profilesByOwner = new HashMap<>();

        @Override
        public Optional<MedicalProfile> findById(Long id) {
            return Optional.empty();
        }

        @Override
        public Optional<MedicalProfile> findByOwner(
                MedicalProfileOwnerType ownerType,
                Long ownerId
        ) {
            return Optional.ofNullable(profilesByOwner.get(key(ownerType, ownerId)));
        }

        @Override
        public List<MedicalProfile> findAll(int page, int size) {
            return profilesByOwner.values().stream().toList();
        }

        @Override
        public MedicalProfile save(MedicalProfile medicalProfile) {
            profilesByOwner.put(
                    key(medicalProfile.getOwnerType(), medicalProfile.getOwnerId()),
                    medicalProfile
            );
            return medicalProfile;
        }

        private String key(MedicalProfileOwnerType ownerType, Long ownerId) {
            return ownerType.name() + ":" + ownerId;
        }
    }
}
