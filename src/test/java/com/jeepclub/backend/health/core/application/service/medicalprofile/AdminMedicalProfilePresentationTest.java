package com.jeepclub.backend.health.core.application.service.medicalprofile;

import com.jeepclub.backend.dependents.api.module.DependentsQuery;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileOwnerInactiveException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileOwnerNotFoundException;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import com.jeepclub.backend.health.core.port.*;
import com.jeepclub.backend.health.core.repository.MedicalProfileRepository;
import com.jeepclub.backend.health.infra.integration.owner.MedicalProfileOwnerPresentationAdapter;
import com.jeepclub.backend.iam.identity.api.module.UserDetails;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.LongStream;

import static com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminMedicalProfilePresentationTest {
    private final MedicalProfileRepository repository = mock(MedicalProfileRepository.class);
    private final MedicalProfileOwnerStatusChecker status = mock(MedicalProfileOwnerStatusChecker.class);
    private final MedicalProfileActiveOwnersQuery active = mock(MedicalProfileActiveOwnersQuery.class);
    private final UserQuery users = mock(UserQuery.class);
    private final DependentsQuery dependents = mock(DependentsQuery.class);
    private final MedicalProfileOwnerPresentationAdapter adapter = new MedicalProfileOwnerPresentationAdapter(users, dependents);
    private final AdminMedicalProfileService service = new AdminMedicalProfileService(repository, status, active, Clock.systemUTC(), adapter);

    @Test
    void mixedFiftyItemPageResolvesEachModuleOnceAndDeduplicatesResponsibleIds() {
        List<MedicalProfile> profiles = new ArrayList<>();
        LongStream.rangeClosed(1, 25).forEach(id -> profiles.add(profile(USER, id)));
        LongStream.rangeClosed(1, 25).forEach(id -> profiles.add(profile(DEPENDENT, id)));
        var pageable = PageRequest.of(0, 50, Sort.by("id").descending());
        when(repository.findAll(any())).thenReturn(new PageImpl<>(profiles));
        when(active.findActiveOwnerIds(any(), any())).thenAnswer(i -> new HashSet<>(i.<Collection<Long>>getArgument(1)));
        when(dependents.findDetailsByIds(any())).thenReturn(LongStream.rangeClosed(1, 25)
                .mapToObj(id -> dependent(id, 1L, "Dependent " + id, "ACTIVE")).toList());
        when(users.findByIds(any())).thenReturn(LongStream.rangeClosed(1, 25)
                .mapToObj(id -> user(id, "Same name", true)).toList());

        var result = service.listPresentations(pageable);
        assertThat(result.getTotalElements()).isEqualTo(50);
        assertThat(result.getPageable()).isEqualTo(pageable);
        assertThat(result.getContent()).extracting(p -> p.profile()).containsExactlyElementsOf(profiles);
        assertThat(result.getContent().subList(0, 25)).allSatisfy(p -> {
            assertThat(p.names().ownerName()).isEqualTo("Same name");
            assertThat(p.names().responsibleName()).isNull();
        });
        assertThat(result.getContent().get(25).names().ownerName()).isEqualTo("Dependent 1");
        assertThat(result.getContent().get(49).names().responsibleName()).isEqualTo("Same name");
        verify(users).findByIds(argThat(ids -> ids.size() == 25 && ids.containsAll(LongStream.rangeClosed(1, 25).boxed().toList())));
        verify(dependents).findDetailsByIds(argThat(ids -> ids.size() == 25));
        verifyNoMoreInteractions(users, dependents);
    }

    @Test
    void secondPageOnlyResolvesFinalEligiblePageAndKeepsTotalAndOrder() {
        var profiles = List.of(profile(USER, 1L), profile(USER, 2L), profile(DEPENDENT, 3L), profile(USER, 4L));
        when(repository.findAll(any())).thenReturn(new PageImpl<>(profiles));
        when(active.findActiveOwnerIds(USER, Set.of(1L, 2L, 4L))).thenReturn(Set.of(1L, 2L));
        when(active.findActiveOwnerIds(DEPENDENT, Set.of(3L))).thenReturn(Set.of(3L));
        when(dependents.findDetailsByIds(argThat(ids -> ids != null && new HashSet<>(ids).equals(Set.of(3L))))).thenReturn(List.of(dependent(3L, 1L, "Child", "ACTIVE")));
        when(users.findByIds(argThat(ids -> ids != null && new HashSet<>(ids).equals(Set.of(1L))))).thenReturn(List.of(user(1L, "Responsible", true)));
        var result = service.listPresentations(PageRequest.of(1, 2));
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getContent()).singleElement().satisfies(p -> assertThat(p.profile()).isSameAs(profiles.get(2)));
        verify(users).findByIds(argThat(ids -> ids != null && new HashSet<>(ids).equals(Set.of(1L))));
        verifyNoMoreInteractions(users);
    }

    @Test
    void emptyAndOutOfRangePagesDoNotConsultPresentationContracts() {
        when(repository.findAll(any())).thenReturn(new PageImpl<>(List.of()));
        assertThat(service.listPresentations(PageRequest.of(0, 50))).isEmpty();
        when(repository.findAll(any())).thenReturn(new PageImpl<>(List.of(profile(USER, 1L))));
        when(active.findActiveOwnerIds(USER, Set.of(1L))).thenReturn(Set.of(1L));
        var result = service.listPresentations(PageRequest.of(1, 50));
        assertThat(result).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(1);
        verifyNoInteractions(users, dependents);
    }

    @ParameterizedTest
    @EnumSource(value = MedicalProfileOwnerStatus.class, names = {"INACTIVE", "NOT_FOUND"})
    void eligibilityFailurePrecedesAnyNameResolution(MedicalProfileOwnerStatus state) {
        when(status.getStatus(USER, 1L)).thenReturn(state);
        when(repository.findById(5L)).thenReturn(Optional.of(profile(USER, 1L)));
        var expected = state == MedicalProfileOwnerStatus.INACTIVE ? MedicalProfileOwnerInactiveException.class : MedicalProfileOwnerNotFoundException.class;
        assertThatThrownBy(() -> service.getPresentationByOwner(USER, 1L)).isInstanceOf(expected);
        assertThatThrownBy(() -> service.getPresentationById(5L)).isInstanceOf(expected);
        verifyNoInteractions(users, dependents);
    }

    @Test
    void identityAndDependentNamesAreReadAgainAndJoinedByTechnicalId() {
        var profile = profile(DEPENDENT, 3L);
        when(status.getStatus(DEPENDENT, 3L)).thenReturn(MedicalProfileOwnerStatus.ACTIVE);
        when(repository.findByOwner(DEPENDENT, 3L)).thenReturn(Optional.of(profile));
        when(dependents.findDetailsByIds(argThat(ids -> ids != null && new HashSet<>(ids).equals(Set.of(3L))))).thenReturn(List.of(dependent(3L, 2L, "Old child", "ACTIVE")))
                .thenReturn(List.of(dependent(3L, 1L, "New child", "ACTIVE")));
        when(users.findByIds(argThat(ids -> ids != null && new HashSet<>(ids).equals(Set.of(2L))))).thenReturn(List.of(user(2L, "First responsible", true)));
        when(users.findByIds(argThat(ids -> ids != null && new HashSet<>(ids).equals(Set.of(1L))))).thenReturn(List.of(user(1L, "New responsible", true)));
        assertThat(service.getPresentationByOwner(DEPENDENT, 3L).names())
                .isEqualTo(new MedicalProfileOwnerPresentationQuery.Names("Old child", "First responsible"));
        assertThat(service.getPresentationByOwner(DEPENDENT, 3L).names())
                .isEqualTo(new MedicalProfileOwnerPresentationQuery.Names("New child", "New responsible"));
    }

    @Test
    void sameNamesDoNotAssociateDifferentResponsibleUsers() {
        when(dependents.findDetailsByIds(any())).thenReturn(List.of(dependent(1L, 10L, "Same child", "ACTIVE"), dependent(2L, 20L, "Same child", "ACTIVE")));
        when(users.findByIds(any())).thenReturn(List.of(user(20L, "Responsible B", true), user(10L, "Responsible A", true)));
        var a = new MedicalProfileOwnerPresentationQuery.Owner(DEPENDENT, 1L);
        var b = new MedicalProfileOwnerPresentationQuery.Owner(DEPENDENT, 2L);
        var result = adapter.resolve(List.of(a, b));
        assertThat(result.get(a).responsibleName()).isEqualTo("Responsible A");
        assertThat(result.get(b).responsibleName()).isEqualTo("Responsible B");
    }

    @Test
    void missingAndNewlyInactiveRecordsFailWithExistingOwnerErrors() {
        var owner = new MedicalProfileOwnerPresentationQuery.Owner(USER, 1L);
        assertThatThrownBy(() -> adapter.resolve(List.of(owner))).isInstanceOf(MedicalProfileOwnerNotFoundException.class);
        when(users.findByIds(any())).thenReturn(List.of(user(1L, "User", false)));
        assertThatThrownBy(() -> adapter.resolve(List.of(owner))).isInstanceOf(MedicalProfileOwnerInactiveException.class);
        var dependent = new MedicalProfileOwnerPresentationQuery.Owner(DEPENDENT, 2L);
        assertThatThrownBy(() -> adapter.resolve(List.of(dependent))).isInstanceOf(MedicalProfileOwnerNotFoundException.class);
        when(dependents.findDetailsByIds(any())).thenReturn(List.of(dependent(2L, 9L, "Child", "INACTIVE")));
        assertThatThrownBy(() -> adapter.resolve(List.of(dependent))).isInstanceOf(MedicalProfileOwnerInactiveException.class);
        when(dependents.findDetailsByIds(any())).thenReturn(List.of(dependent(2L, 9L, "Child", "ACTIVE")));
        assertThatThrownBy(() -> adapter.resolve(List.of(dependent))).isInstanceOf(MedicalProfileOwnerNotFoundException.class);
    }

    @Test
    void inactiveResponsibleDoesNotChangeActiveDependentEligibility() {
        when(dependents.findDetailsByIds(any())).thenReturn(List.of(dependent(2L, 9L, "Child", "ACTIVE")));
        when(users.findByIds(any())).thenReturn(List.of(user(9L, "Responsible", false)));
        assertThat(adapter.resolve(List.of(new MedicalProfileOwnerPresentationQuery.Owner(DEPENDENT, 2L))).values())
                .containsExactly(new MedicalProfileOwnerPresentationQuery.Names("Child", "Responsible"));
    }

    @Test
    void presentationRespectsBothPublicBatchLimits() {
        var owners = LongStream.rangeClosed(1, 501).mapToObj(id -> new MedicalProfileOwnerPresentationQuery.Owner(DEPENDENT, id)).toList();
        when(dependents.findDetailsByIds(any())).thenAnswer(i -> i.<Collection<Long>>getArgument(0).stream()
                .map(id -> dependent(id, id, "Child", "ACTIVE")).toList());
        when(users.findByIds(any())).thenAnswer(i -> i.<Collection<Long>>getArgument(0).stream().map(id -> user(id, "User", true)).toList());
        assertThat(adapter.resolve(owners)).hasSize(501);
        verify(dependents, times(2)).findDetailsByIds(argThat(ids -> ids.size() <= 500));
        verify(users, times(2)).findByIds(argThat(ids -> ids.size() <= 500));
        verifyNoMoreInteractions(users, dependents);
    }

    private MedicalProfile profile(MedicalProfileOwnerType type, Long id) {
        return MedicalProfile.create(type, id, null, "CLINICAL_SENTINEL", null, null, null, null, null, null, null, null, null, Instant.EPOCH);
    }
    private DependentsQuery.Details dependent(Long id, Long userId, String name, String status) {
        return new DependentsQuery.Details(id, userId, name, "PRIVATE_CPF", "CHILD", status);
    }
    private UserDetails user(Long id, String name, boolean active) {
        return new UserDetails(id, name, null, "PRIVATE_EMAIL", "PRIVATE_CPF", null, "PRIVATE_PHONE", null, active, Instant.EPOCH, null, Instant.EPOCH);
    }
}
