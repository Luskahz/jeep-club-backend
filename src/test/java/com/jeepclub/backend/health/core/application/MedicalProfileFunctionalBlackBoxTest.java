package com.jeepclub.backend.health.core.application;

import com.jeepclub.backend.health.api.http.dto.MedicalProfileRequest;
import com.jeepclub.backend.health.core.application.exceptions.InvalidMedicalProfileDataException;
import com.jeepclub.backend.health.core.application.service.medicalprofile.MedicalProfileService;
import com.jeepclub.backend.health.core.application.service.medicalprofile.internal.MedicalProfileManager;
import com.jeepclub.backend.health.core.domain.enums.BloodType;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import com.jeepclub.backend.health.core.port.DependentOwnershipChecker;
import com.jeepclub.backend.health.core.repository.MedicalProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MedicalProfileFunctionalBlackBoxTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-06-30T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    private MedicalProfileService service;

    @BeforeEach
    void setUp() {
        MedicalProfileRepository repository = new InMemoryMedicalProfileRepository();
        service = new MedicalProfileService(
                new MedicalProfileManager(repository, FIXED_CLOCK),
                Optional.<DependentOwnershipChecker>empty()
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "(12) 99999-9999",
            "12 99999 9999",
            "12999999999",
            "1233334444"
    })
    void deveAceitarTelefonesValidosPorClasseDeEquivalencia(String validPhone) {
        MedicalProfileRequest request = requestWithPhone(validPhone);

        MedicalProfile profile = service.upsertMyMedicalProfile(10L, request.toApplicationData());

        assertEquals(validPhone.replaceAll("\\D", ""), profile.getEmergencyContactPhone());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "123",
            "999999999",
            "129999999999"
    })
    void deveRejeitarTelefonesInvalidosPorClasseDeEquivalencia(String invalidPhone) {
        MedicalProfileRequest request = requestWithPhone(invalidPhone);

        assertThrows(
                InvalidMedicalProfileDataException.class,
                () -> service.upsertMyMedicalProfile(10L, request.toApplicationData())
        );
    }

    @Test
    void deveTratarTelefoneSemDigitosComoNaoInformado() {
        MedicalProfileRequest request = requestWithPhone("telefone-invalido");

        MedicalProfile profile = service.upsertMyMedicalProfile(10L, request.toApplicationData());

        assertNull(profile.getEmergencyContactPhone());
    }

    @Test
    void deveAceitarPerfilMedicoSemTipoSanguineoInformadoComoUnknown() {
        MedicalProfileRequest request = new MedicalProfileRequest(
                null,
                "Dipirona",
                "Asma",
                "Bombinha",
                "Unimed",
                "Enfermaria",
                "123456789",
                "Maria",
                "(12) 99999-9999",
                "Mãe",
                "Observação"
        );

        MedicalProfile profile = service.upsertMyMedicalProfile(10L, request.toApplicationData());

        assertEquals(BloodType.UNKNOWN, profile.getBloodType());
    }

    private MedicalProfileRequest requestWithPhone(String phone) {
        return new MedicalProfileRequest(
                BloodType.O_POSITIVE,
                "Dipirona",
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

        private MedicalProfile storedProfile;

        @Override
        public Optional<MedicalProfile> findById(Long id) {
            return Optional.empty();
        }

        @Override
        public Optional<MedicalProfile> findByOwner(MedicalProfileOwnerType ownerType, Long ownerId) {
            if (storedProfile == null) {
                return Optional.empty();
            }

            boolean sameOwner = storedProfile.getOwnerType() == ownerType
                    && storedProfile.getOwnerId().equals(ownerId);

            return sameOwner ? Optional.of(storedProfile) : Optional.empty();
        }

        @Override
        public List<MedicalProfile> findAll(int page, int size) {
            return storedProfile == null ? List.of() : List.of(storedProfile);
        }

        @Override
        public MedicalProfile save(MedicalProfile medicalProfile) {
            this.storedProfile = medicalProfile;
            return medicalProfile;
        }
    }
}
