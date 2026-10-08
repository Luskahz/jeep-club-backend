package com.jeepclub.backend.health.core.application.service.medicalprofile;

import com.jeepclub.backend.health.core.application.command.UpsertMedicalProfileCommand;
import com.jeepclub.backend.health.core.application.exceptions.*;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import com.jeepclub.backend.health.core.port.*;
import com.jeepclub.backend.health.core.repository.MedicalProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.*;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MedicalProfileAccessBoundaryTest {
    private static final Instant NOW = Instant.parse("2026-09-20T12:00:00Z");
    private final MedicalProfileRepository repository = mock(MedicalProfileRepository.class);
    private final MedicalProfileOwnerStatusChecker owners = mock(MedicalProfileOwnerStatusChecker.class);
    private final DependentOwnershipChecker ownership = mock(DependentOwnershipChecker.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final MedicalProfileService member = new MedicalProfileService(repository, ownership, owners, clock);
    private final AdminMedicalProfileService admin = new AdminMedicalProfileService(repository, owners,
            mock(MedicalProfileActiveOwnersQuery.class), clock);

    @ParameterizedTest
    @EnumSource(value = MedicalProfileOwnerStatus.class, names = {"INACTIVE", "NOT_FOUND"})
    void memberOwnWritesAndDeletesValidateOwnerBeforeTouchingProfile(MedicalProfileOwnerStatus status) {
        when(owners.getStatus(MedicalProfileOwnerType.USER, 1L)).thenReturn(status);
        Class<? extends RuntimeException> expected = status == MedicalProfileOwnerStatus.INACTIVE
                ? MedicalProfileOwnerInactiveException.class : MedicalProfileOwnerNotFoundException.class;
        assertThatThrownBy(() -> member.upsertMyMedicalProfile(1L, command())).isInstanceOf(expected);
        assertThatThrownBy(() -> member.deleteMyMedicalProfile(1L)).isInstanceOf(expected);
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void administrativeIdentifiersMustBePositiveBeforeAnyIntegration(Long id) {
        assertThatThrownBy(() -> admin.getById(id)).isInstanceOf(InvalidMedicalProfileDataException.class);
        assertThatThrownBy(() -> admin.getByOwner(MedicalProfileOwnerType.USER, id)).isInstanceOf(InvalidMedicalProfileDataException.class);
        assertThatThrownBy(() -> admin.upsertByOwner(MedicalProfileOwnerType.USER, id, command())).isInstanceOf(InvalidMedicalProfileDataException.class);
        assertThatThrownBy(() -> admin.deleteById(id, 1L)).isInstanceOf(InvalidMedicalProfileDataException.class);
        assertThatThrownBy(() -> admin.deleteById(1L, id)).isInstanceOf(InvalidMedicalProfileDataException.class);
        verifyNoInteractions(repository, owners);
    }

    @Test
    void administrativeOwnerTypeIsRequiredOnReadAndUpsert() {
        assertThatThrownBy(() -> admin.getByOwner(null, 1L)).isInstanceOf(InvalidMedicalProfileDataException.class);
        assertThatThrownBy(() -> admin.upsertByOwner(null, 1L, command())).isInstanceOf(InvalidMedicalProfileDataException.class);
        verifyNoInteractions(repository, owners);
    }

    @ParameterizedTest
    @EnumSource(MedicalProfileOwnerType.class)
    void administrativeReadsReturnActiveProfileAndDistinguishMissingProfileFromMissingOwner(MedicalProfileOwnerType type) {
        when(owners.getStatus(type, 1L)).thenReturn(MedicalProfileOwnerStatus.ACTIVE);
        var profile = MedicalProfile.create(type, 1L, null, null, null, null, null, null, null, null, null, null, null, NOW);
        when(repository.findByOwner(type, 1L)).thenReturn(Optional.of(profile));
        when(repository.findById(2L)).thenReturn(Optional.of(profile));
        assertThat(admin.getByOwner(type, 1L)).isSameAs(profile);
        assertThat(admin.getById(2L)).isSameAs(profile);
        when(repository.findByOwner(type, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> admin.getByOwner(type, 1L)).isInstanceOf(MedicalProfileNotFoundException.class);
        when(owners.getStatus(type, 1L)).thenReturn(MedicalProfileOwnerStatus.NOT_FOUND);
        assertThatThrownBy(() -> admin.getByOwner(type, 1L)).isInstanceOf(MedicalProfileOwnerNotFoundException.class);
        when(owners.getStatus(type, 1L)).thenReturn(MedicalProfileOwnerStatus.INACTIVE);
        assertThatThrownBy(() -> admin.getByOwner(type, 1L)).isInstanceOf(MedicalProfileOwnerInactiveException.class);
    }

    @Test
    void dependentCreationAndUpdateUseDependentIdentityAndLockWhilePreservingCreationDate() {
        when(owners.getStatus(MedicalProfileOwnerType.USER, 1L)).thenReturn(MedicalProfileOwnerStatus.ACTIVE);
        when(owners.getStatus(MedicalProfileOwnerType.DEPENDENT, 2L)).thenReturn(MedicalProfileOwnerStatus.ACTIVE);
        when(ownership.belongsToUser(2L, 1L)).thenReturn(true);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        var created = member.upsertDependentMedicalProfile(1L, 2L, command());
        assertThat(created.getOwnerType()).isEqualTo(MedicalProfileOwnerType.DEPENDENT);
        assertThat(created.getOwnerId()).isEqualTo(2L);
        assertThat(created.getCreatedAt()).isEqualTo(NOW);
        when(repository.findByOwner(MedicalProfileOwnerType.DEPENDENT, 2L)).thenReturn(Optional.of(created));
        assertThat(member.getDependentMedicalProfile(1L, 2L)).isSameAs(created);
        when(repository.findByOwnerForUpdate(MedicalProfileOwnerType.DEPENDENT, 2L)).thenReturn(Optional.of(created));
        assertThat(member.upsertDependentMedicalProfile(1L, 2L, command())).isSameAs(created);
        verify(repository, times(2)).findByOwnerForUpdate(MedicalProfileOwnerType.DEPENDENT, 2L);
        verify(repository, never()).findByOwnerForUpdate(MedicalProfileOwnerType.USER, 1L);
    }

    private UpsertMedicalProfileCommand command() {
        return new UpsertMedicalProfileCommand(null, null, null, null, null, null, null, null, null, null, null);
    }
}
