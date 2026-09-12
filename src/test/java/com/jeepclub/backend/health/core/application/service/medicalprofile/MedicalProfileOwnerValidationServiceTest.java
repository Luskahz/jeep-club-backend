package com.jeepclub.backend.health.core.application.service.medicalprofile;

import com.jeepclub.backend.health.core.application.command.UpsertMedicalProfileCommand;
import com.jeepclub.backend.health.core.application.exceptions.InvalidMedicalProfileDataException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileAccessDeniedException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileOwnerInactiveException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileOwnerNotFoundException;
import com.jeepclub.backend.health.core.domain.enums.BloodType;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import com.jeepclub.backend.health.core.port.DependentOwnershipChecker;
import com.jeepclub.backend.health.core.port.MedicalProfileOwnerStatus;
import com.jeepclub.backend.health.core.port.MedicalProfileOwnerStatusChecker;
import com.jeepclub.backend.health.core.repository.MedicalProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalProfileOwnerValidationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-07T12:00:00Z");

    @Mock
    private MedicalProfileRepository repository;
    @Mock
    private DependentOwnershipChecker ownershipChecker;
    @Mock
    private MedicalProfileOwnerStatusChecker statusChecker;

    private MedicalProfileService memberService;
    private AdminMedicalProfileService adminService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.plusSeconds(60), ZoneOffset.UTC);
        memberService = new MedicalProfileService(
                repository,
                ownershipChecker,
                statusChecker,
                clock
        );
        adminService = new AdminMedicalProfileService(
                repository,
                statusChecker,
                clock
        );
    }

    @Test
    void activeUserCanConsultOwnProfile() {
        MedicalProfile profile = profile(MedicalProfileOwnerType.USER, 7L);
        when(statusChecker.getStatus(MedicalProfileOwnerType.USER, 7L))
                .thenReturn(MedicalProfileOwnerStatus.ACTIVE);
        when(repository.findByOwner(MedicalProfileOwnerType.USER, 7L))
                .thenReturn(Optional.of(profile));

        assertThat(memberService.getMyMedicalProfile(7L)).isSameAs(profile);
    }

    @Test
    void invalidOwnerIdIsRejectedBeforeConsultingIntegrations() {
        assertThatThrownBy(() -> memberService.getMyMedicalProfile(0L))
                .isInstanceOf(InvalidMedicalProfileDataException.class);

        verify(statusChecker, never()).getStatus(
                MedicalProfileOwnerType.USER,
                0L
        );
    }

    @Test
    void nonexistentOwnerCannotBeCreatedByAdministrator() {
        when(statusChecker.getStatus(MedicalProfileOwnerType.USER, 404L))
                .thenReturn(MedicalProfileOwnerStatus.NOT_FOUND);

        assertThatThrownBy(() -> adminService.upsertByOwner(
                MedicalProfileOwnerType.USER,
                404L,
                command()
        )).isInstanceOf(MedicalProfileOwnerNotFoundException.class);

        verify(repository, never()).findByOwnerForUpdate(
                MedicalProfileOwnerType.USER,
                404L
        );
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void inactiveDependentCannotBeConsultedOrUpdated() {
        when(statusChecker.getStatus(MedicalProfileOwnerType.USER, 7L))
                .thenReturn(MedicalProfileOwnerStatus.ACTIVE);
        when(statusChecker.getStatus(MedicalProfileOwnerType.DEPENDENT, 11L))
                .thenReturn(MedicalProfileOwnerStatus.INACTIVE);

        assertThatThrownBy(() -> memberService.getDependentMedicalProfile(7L, 11L))
                .isInstanceOf(MedicalProfileOwnerInactiveException.class);
        assertThatThrownBy(() -> memberService.upsertDependentMedicalProfile(
                7L,
                11L,
                command()
        )).isInstanceOf(MedicalProfileOwnerInactiveException.class);

        verify(ownershipChecker, never()).belongsToUser(11L, 7L);
        verify(repository, never()).findByOwner(
                MedicalProfileOwnerType.DEPENDENT,
                11L
        );
        verify(repository, never()).findByOwnerForUpdate(
                MedicalProfileOwnerType.DEPENDENT,
                11L
        );
    }

    @Test
    void activeDependentWithDeniedOwnershipReturnsAccessDenied() {
        when(statusChecker.getStatus(MedicalProfileOwnerType.USER, 7L))
                .thenReturn(MedicalProfileOwnerStatus.ACTIVE);
        when(statusChecker.getStatus(MedicalProfileOwnerType.DEPENDENT, 11L))
                .thenReturn(MedicalProfileOwnerStatus.ACTIVE);
        when(ownershipChecker.belongsToUser(11L, 7L)).thenReturn(false);

        assertThatThrownBy(() -> memberService.getDependentMedicalProfile(7L, 11L))
                .isInstanceOf(MedicalProfileAccessDeniedException.class);
    }

    @Test
    void administrativeConsultationByProfileIdBlocksInactiveOwner() {
        MedicalProfile profile = profile(MedicalProfileOwnerType.DEPENDENT, 11L);
        when(repository.findById(1L)).thenReturn(Optional.of(profile));
        when(statusChecker.getStatus(MedicalProfileOwnerType.DEPENDENT, 11L))
                .thenReturn(MedicalProfileOwnerStatus.INACTIVE);

        assertThatThrownBy(() -> adminService.getById(1L))
                .isInstanceOf(MedicalProfileOwnerInactiveException.class);
    }

    @Test
    void administrativeListUsesAlreadyFilteredPageWithoutOwnerLookups() {
        var pageable = PageRequest.of(0, 20);
        MedicalProfile active = profile(MedicalProfileOwnerType.USER, 7L);
        when(repository.findAllWithActiveOwners(pageable)).thenReturn(
                new PageImpl<>(List.of(active), pageable, 1)
        );

        var result = adminService.listMedicalProfiles(pageable);

        assertThat(result.getContent()).containsExactly(active);
        assertThat(result.getNumber()).isZero();
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(statusChecker, never()).getStatus(
                MedicalProfileOwnerType.USER,
                7L
        );
    }

    @Test
    void administrativeListPreservesFilteredTotalsAndSecondPage() {
        var firstPage = PageRequest.of(0, 2);
        var secondPage = PageRequest.of(1, 2);
        MedicalProfile activeUser = profile(MedicalProfileOwnerType.USER, 7L);
        MedicalProfile activeDependent = profile(MedicalProfileOwnerType.DEPENDENT, 11L);
        MedicalProfile anotherActiveUser = profile(MedicalProfileOwnerType.USER, 8L);

        when(repository.findAllWithActiveOwners(firstPage)).thenReturn(
                new PageImpl<>(List.of(activeUser, activeDependent), firstPage, 3)
        );
        when(repository.findAllWithActiveOwners(secondPage)).thenReturn(
                new PageImpl<>(List.of(anotherActiveUser), secondPage, 3)
        );

        var first = adminService.listMedicalProfiles(firstPage);
        var second = adminService.listMedicalProfiles(secondPage);

        assertThat(first.getContent()).containsExactly(activeUser, activeDependent);
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(second.getContent()).containsExactly(anotherActiveUser);
        assertThat(second.getNumber()).isEqualTo(1);
        assertThat(second.getTotalElements()).isEqualTo(3);
        assertThat(second.getTotalPages()).isEqualTo(2);
    }

    @Test
    void administrativeListReturnsEmptyPageWhenNoOwnerIsEligible() {
        var pageable = PageRequest.of(0, 20);
        when(repository.findAllWithActiveOwners(pageable)).thenReturn(
                new PageImpl<>(List.of(), pageable, 0)
        );

        var result = adminService.listMedicalProfiles(pageable);

        assertThat(result).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
    }

    @Test
    void administratorCanDeleteRetainedProfileAfterOwnerRemoval() {
        MedicalProfile orphan = profile(MedicalProfileOwnerType.USER, 404L);
        when(repository.findById(1L)).thenReturn(Optional.of(orphan));

        adminService.deleteById(1L, 99L);

        verify(repository).delete(orphan, 99L, NOW.plusSeconds(60));
        verify(statusChecker, never()).getStatus(
                MedicalProfileOwnerType.USER,
                404L
        );
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
                "Dipirona",
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

    private UpsertMedicalProfileCommand command() {
        return new UpsertMedicalProfileCommand(
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
                null
        );
    }
}
