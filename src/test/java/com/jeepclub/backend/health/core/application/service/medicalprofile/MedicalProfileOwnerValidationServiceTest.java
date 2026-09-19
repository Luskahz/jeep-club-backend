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
import com.jeepclub.backend.health.core.port.MedicalProfileActiveOwnersQuery;
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
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
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
    @Mock
    private MedicalProfileActiveOwnersQuery activeOwnersQuery;

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
                activeOwnersQuery,
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
    void administrativeListPaginatesOnlyActiveOwnersWithExactTotals() {
        var firstPage = PageRequest.of(0, 2);
        var secondPage = PageRequest.of(1, 2);
        MedicalProfile activeUser = profile(MedicalProfileOwnerType.USER, 7L);
        MedicalProfile inactiveUser = profile(MedicalProfileOwnerType.USER, 8L);
        MedicalProfile activeDependent = profile(MedicalProfileOwnerType.DEPENDENT, 11L);
        MedicalProfile inactiveDependent = profile(MedicalProfileOwnerType.DEPENDENT, 12L);
        MedicalProfile missingUser = profile(MedicalProfileOwnerType.USER, 404L);
        MedicalProfile anotherActiveUser = profile(MedicalProfileOwnerType.USER, 9L);
        List<MedicalProfile> profiles = List.of(
                activeUser,
                inactiveUser,
                activeDependent,
                inactiveDependent,
                missingUser,
                anotherActiveUser
        );

        pagesFrom(profiles);
        when(activeOwnersQuery.findActiveOwnerIds(
                eq(MedicalProfileOwnerType.USER),
                org.mockito.ArgumentMatchers.anyCollection()
        )).thenReturn(java.util.Set.of(7L, 9L));
        when(activeOwnersQuery.findActiveOwnerIds(
                eq(MedicalProfileOwnerType.DEPENDENT),
                org.mockito.ArgumentMatchers.anyCollection()
        )).thenReturn(java.util.Set.of(11L));

        var first = adminService.listMedicalProfiles(firstPage);
        var second = adminService.listMedicalProfiles(secondPage);

        assertThat(first.getContent()).containsExactly(activeUser, activeDependent);
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(second.getContent()).containsExactly(anotherActiveUser);
        assertThat(second.getNumber()).isEqualTo(1);
        assertThat(second.getTotalElements()).isEqualTo(3);
        assertThat(second.getTotalPages()).isEqualTo(2);
        verify(statusChecker, never()).getStatus(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void administrativeListReturnsEmptyPageWhenNoOwnerIsEligibleWithoutNPlusOne() {
        var pageable = PageRequest.of(0, 20);
        pagesFrom(List.of(
                profile(MedicalProfileOwnerType.USER, 7L),
                profile(MedicalProfileOwnerType.DEPENDENT, 11L),
                profile(MedicalProfileOwnerType.USER, 404L)
        ));
        when(activeOwnersQuery.findActiveOwnerIds(
                eq(MedicalProfileOwnerType.USER),
                org.mockito.ArgumentMatchers.anyCollection()
        )).thenReturn(java.util.Set.of());
        when(activeOwnersQuery.findActiveOwnerIds(
                eq(MedicalProfileOwnerType.DEPENDENT),
                org.mockito.ArgumentMatchers.anyCollection()
        )).thenReturn(java.util.Set.of());

        var result = adminService.listMedicalProfiles(pageable);

        assertThat(result).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
        verify(activeOwnersQuery).findActiveOwnerIds(
                MedicalProfileOwnerType.USER,
                java.util.Set.of(7L, 404L)
        );
        verify(activeOwnersQuery).findActiveOwnerIds(
                MedicalProfileOwnerType.DEPENDENT,
                java.util.Set.of(11L)
        );
    }

    @Test
    void administrativeListUsesOneBatchLookupPerOwnerTypeForEachSourcePage() {
        List<MedicalProfile> profiles = java.util.stream.IntStream.rangeClosed(1, 101)
                .mapToObj(id -> profile(
                        id % 2 == 0
                                ? MedicalProfileOwnerType.USER
                                : MedicalProfileOwnerType.DEPENDENT,
                        (long) id
                ))
                .toList();
        pagesFrom(profiles);
        when(activeOwnersQuery.findActiveOwnerIds(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyCollection()
        )).thenAnswer(invocation -> invocation.getArgument(1));

        var result = adminService.listMedicalProfiles(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(10);
        assertThat(result.getTotalElements()).isEqualTo(101);
        verify(activeOwnersQuery, times(2)).findActiveOwnerIds(
                eq(MedicalProfileOwnerType.USER),
                org.mockito.ArgumentMatchers.anyCollection()
        );
        verify(activeOwnersQuery, times(2)).findActiveOwnerIds(
                eq(MedicalProfileOwnerType.DEPENDENT),
                org.mockito.ArgumentMatchers.anyCollection()
        );
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

    private void pagesFrom(List<MedicalProfile> profiles) {
        when(repository.findAll(any(Pageable.class))).thenAnswer(invocation -> {
            Pageable requested = invocation.getArgument(0);
            int start = (int) requested.getOffset();
            int end = Math.min(start + requested.getPageSize(), profiles.size());
            List<MedicalProfile> content = start >= profiles.size()
                    ? List.of()
                    : profiles.subList(start, end);
            return new PageImpl<>(content, requested, profiles.size());
        });
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
