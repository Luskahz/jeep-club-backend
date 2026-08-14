package com.jeepclub.backend.health.core.application;

import com.jeepclub.backend.health.api.http.dto.MedicalProfileRequest;
import com.jeepclub.backend.health.core.application.exceptions.InvalidMedicalProfileDataException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileNotFoundException;
import com.jeepclub.backend.health.core.application.service.medicalprofile.MedicalProfileService;
import com.jeepclub.backend.health.core.application.service.medicalprofile.internal.MedicalProfileManager;
import com.jeepclub.backend.health.core.domain.enums.BloodType;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import com.jeepclub.backend.health.core.port.DependentOwnershipChecker;
import com.jeepclub.backend.health.core.repository.MedicalProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalProfileServiceStructuralWhiteBoxTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-06-30T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    @Mock
    private MedicalProfileRepository medicalProfileRepository;

    private MedicalProfileService service;

    @BeforeEach
    void setUp() {
        service = new MedicalProfileService(
                new MedicalProfileManager(medicalProfileRepository, FIXED_CLOCK),
                Optional.<DependentOwnershipChecker>empty()
        );
    }

    @Test
    void upsertQuandoPerfilNaoExisteDeveExecutarRamoDeCriacao() {
        MedicalProfileRequest request = validRequest("(12) 99999-9999");

        when(medicalProfileRepository.findByOwner(MedicalProfileOwnerType.USER, 10L))
                .thenReturn(Optional.empty());
        when(medicalProfileRepository.save(any(MedicalProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MedicalProfile profile = service.upsertMyMedicalProfile(10L, request.toApplicationData());

        assertEquals(MedicalProfileOwnerType.USER, profile.getOwnerType());
        assertEquals(10L, profile.getOwnerId());
        assertEquals("12999999999", profile.getEmergencyContactPhone());
        assertEquals(FIXED_NOW, profile.getCreatedAt());
        assertEquals(FIXED_NOW, profile.getUpdatedAt());
        verify(medicalProfileRepository).save(any(MedicalProfile.class));
    }

    @Test
    void upsertQuandoPerfilExisteDeveExecutarRamoDeAtualizacao() {
        MedicalProfile existing = MedicalProfile.create(
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
                FIXED_NOW
        );

        MedicalProfileRequest request = validRequest("(12) 98888-7777");

        when(medicalProfileRepository.findByOwner(MedicalProfileOwnerType.USER, 10L))
                .thenReturn(Optional.of(existing));
        when(medicalProfileRepository.save(any(MedicalProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MedicalProfile updated = service.upsertMyMedicalProfile(10L, request.toApplicationData());

        assertEquals(BloodType.O_POSITIVE, updated.getBloodType());
        assertEquals("12988887777", updated.getEmergencyContactPhone());
        assertEquals("Dipirona", updated.getAllergies());
        assertEquals(FIXED_NOW, updated.getUpdatedAt());
        verify(medicalProfileRepository).save(existing);
    }

    @Test
    void getByOwnerQuandoPerfilNaoExisteDeveExecutarRamoDeErro() {
        when(medicalProfileRepository.findByOwner(MedicalProfileOwnerType.USER, 10L))
                .thenReturn(Optional.empty());

        assertThrows(
                MedicalProfileNotFoundException.class,
                () -> service.getMyMedicalProfile(10L)
        );
    }

    @Test
    void upsertQuandoOwnerIdForNuloDeveExecutarRamoDeValidacaoEnaoSalvar() {
        MedicalProfileRequest request = validRequest("(12) 99999-9999");

        assertThrows(
                InvalidMedicalProfileDataException.class,
                () -> service.upsertMyMedicalProfile(null, request.toApplicationData())
        );

        verify(medicalProfileRepository, never()).save(any(MedicalProfile.class));
    }

    private MedicalProfileRequest validRequest(String phone) {
        return new MedicalProfileRequest(
                BloodType.O_POSITIVE,
                "  Dipirona  ",
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
}
