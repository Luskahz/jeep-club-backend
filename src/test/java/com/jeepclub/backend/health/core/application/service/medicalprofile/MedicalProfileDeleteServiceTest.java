package com.jeepclub.backend.health.core.application.service.medicalprofile;

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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalProfileDeleteServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-31T12:00:00Z");

    @Mock
    private MedicalProfileRepository repository;
    @Mock
    private DependentOwnershipChecker dependentOwnershipChecker;

    private MedicalProfileService service;
    private AdminMedicalProfileService adminService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new MedicalProfileService(
                repository,
                dependentOwnershipChecker,
                clock
        );
        adminService = new AdminMedicalProfileService(
                repository,
                clock
        );
    }

    @Test
    void memberDeletesOwnProfileWithAuthenticatedUserAsActor() {
        MedicalProfile profile = profile(MedicalProfileOwnerType.USER, 7L);
        when(repository.findByOwner(MedicalProfileOwnerType.USER, 7L))
                .thenReturn(Optional.of(profile));

        service.deleteMyMedicalProfile(7L);

        verify(repository).delete(profile, 7L, NOW);
    }

    @Test
    void memberDeletesOwnedDependentProfile() {
        MedicalProfile profile = profile(MedicalProfileOwnerType.DEPENDENT, 11L);
        when(dependentOwnershipChecker.belongsToUser(11L, 7L))
                .thenReturn(true);
        when(repository.findByOwner(MedicalProfileOwnerType.DEPENDENT, 11L))
                .thenReturn(Optional.of(profile));

        service.deleteDependentMedicalProfile(7L, 11L);

        verify(repository).delete(profile, 7L, NOW);
    }

    @Test
    void administratorDeletesProfileWithOwnActorId() {
        MedicalProfile profile = profile(MedicalProfileOwnerType.USER, 7L);
        when(repository.findById(1L)).thenReturn(Optional.of(profile));

        adminService.deleteById(1L, 99L);

        verify(repository).delete(profile, 99L, NOW);
    }

    private MedicalProfile profile(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    ) {
        return MedicalProfile.reconstitute(
                1L,
                ownerType,
                ownerId,
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
                NOW,
                NOW
        );
    }
}
