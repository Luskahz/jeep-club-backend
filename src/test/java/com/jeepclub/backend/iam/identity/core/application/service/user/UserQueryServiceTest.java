package com.jeepclub.backend.iam.identity.core.application.service.user;

import com.jeepclub.backend.iam.identity.api.module.UserDetails;
import com.jeepclub.backend.iam.identity.api.module.UserStatus;
import com.jeepclub.backend.iam.identity.core.domain.model.User;
import com.jeepclub.backend.iam.identity.core.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.LongStream;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserQueryServiceTest {
    @Test void nameSearchHasAHardLimitAndNeverSilentlyTruncates() {
        when(repository.findIdsByName("literal %_!",501)).thenReturn(LongStream.rangeClosed(1,500).boxed().toList());
        assertThat(query.findIdsByName(" literal %_! ")).hasSize(500);
        when(repository.findIdsByName("broad",501)).thenReturn(LongStream.rangeClosed(1,501).boxed().toList());
        assertThatThrownBy(() -> query.findIdsByName("broad"))
                .isInstanceOf(com.jeepclub.backend.iam.identity.api.module.exception.UserNameSearchTooBroadException.class);
        clearInvocations(repository);
        assertThat(query.findIdsByName(null)).isEmpty();assertThat(query.findIdsByName(" ")).isEmpty();
        assertThatThrownBy(() -> query.findIdsByName("x".repeat(151))).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }
    private static final Instant CREATED = Instant.parse("2026-10-01T12:00:00Z");
    private final UserRepository repository = mock(UserRepository.class);
    private final UserQueryService query = new UserQueryService(repository);

    @Test void emptyBatchesDoNotQueryPersistence() {
        assertThat(query.findByIds(null)).isEmpty();
        assertThat(query.findByIds(List.of())).isEmpty();
        assertThat(query.findAdministrativelyActiveUserIdsByIds(null)).isEmpty();
        assertThat(query.findAdministrativelyActiveUserIdsByIds(Set.of())).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test void acceptsExactly500IdsButRejects501BeforeQuerying() {
        var ids = LongStream.rangeClosed(1, 500).boxed().toList();
        when(repository.findByIds(ids)).thenReturn(List.of());
        assertThat(query.findByIds(ids)).isEmpty();
        verify(repository).findByIds(ids);
        clearInvocations(repository);
        assertThatIllegalArgumentException().isThrownBy(() -> query.findByIds(Collections.nCopies(501, 1L)));
        verifyNoInteractions(repository);
    }

    @ParameterizedTest @CsvSource({"-1,1", "0,0", "0,-1", "0,501"})
    void rejectsInvalidKeysetBeforeQuerying(long after, int limit) {
        assertThatIllegalArgumentException().isThrownBy(() -> query.findAfterId(after, limit));
        verifyNoInteractions(repository);
    }

    @ParameterizedTest @CsvSource({"0,1", "0,500", "9,1"})
    void keysetBoundaryDelegatesCursorAndLimit(long after, int limit) {
        when(repository.findAfterId(after, limit)).thenReturn(List.of(user(UserStatus.ACTIVE)));
        assertThat(query.findAfterId(after, limit)).extracting(UserDetails::id).containsExactly(7L);
        verify(repository).findAfterId(after, limit);
    }

    @ParameterizedTest @EnumSource(UserStatus.class)
    void publicProjectionPreservesAdministrativeStateAndOptionalData(UserStatus status) {
        User source = user(status);
        var expected = new UserDetails(7L, "Query User", LocalDate.of(2000, 1, 1),
                "query@example.com", "52998224725", "12345", "11999999999", null,
                status == UserStatus.ACTIVE, CREATED, source.getDisabledAt(), CREATED.plusSeconds(2),
                status, source.getArchivedAt());
        when(repository.findById(7L)).thenReturn(Optional.of(source));
        when(repository.findByIds(List.of(7L, 999L))).thenReturn(List.of(source));
        when(repository.findByCpf("52998224725")).thenReturn(Optional.of(source));
        assertThat(query.findById(7L)).contains(expected);
        assertThat(query.findByIds(List.of(7L, 999L))).containsExactly(expected);
        assertThat(query.findByCpf("52998224725")).contains(expected);
    }

    @Test void missingUsersRemainAbsentWithoutInventedIdentity() {
        assertThat(query.findById(999L)).isEmpty();
        assertThat(query.findByCpf("00000000999")).isEmpty();
        assertThat(query.existsById(999L)).isFalse();
        assertThat(query.isAdministrativelyActive(999L)).isFalse();
    }

    @Test void uniquenessQueriesNormalizeDocumentsAndIgnoreAbsentContacts() {
        assertThat(query.existsByCpf(null)).isFalse();
        assertThat(query.existsByEmail(null)).isFalse();
        assertThat(query.existsByEmail("  ")).isFalse();
        verifyNoInteractions(repository);
        when(repository.existsByCpf("52998224725")).thenReturn(true);
        when(repository.existsByEmail("query@example.com")).thenReturn(true);
        assertThat(query.existsByCpf("529.982.247-25")).isTrue();
        assertThat(query.existsByEmail(" QUERY@EXAMPLE.COM ")).isTrue();
        assertThat(query.existsByCpf("00000000999")).isFalse();
        assertThat(query.existsByEmail("missing@example.com")).isFalse();
    }

    @Test void administrativeQueriesUseTheRepositoryEligibilityProjection() {
        when(repository.findActiveIds()).thenReturn(List.of(7L));
        when(repository.findActiveIdsByIds(List.of(7L, 8L))).thenReturn(Set.of(7L));
        when(repository.existsActiveById(7L)).thenReturn(true);
        when(repository.existsById(7L)).thenReturn(true);
        assertThat(query.findAdministrativelyActiveUserIds()).containsExactly(7L);
        assertThat(query.findAdministrativelyActiveUserIdsByIds(List.of(7L, 8L))).containsExactly(7L);
        assertThat(query.isAdministrativelyActive(7L)).isTrue();
        assertThat(query.existsById(7L)).isTrue();
    }

    private User user(UserStatus status) {
        return User.reconstitute(7L, "Query User", LocalDate.of(2000, 1, 1), "query@example.com",
                "52998224725", "12345", "11999999999", null, status, CREATED,
                status == UserStatus.ACTIVE ? null : CREATED.plusSeconds(1), CREATED.plusSeconds(2),
                status == UserStatus.ARCHIVED ? CREATED.plusSeconds(2) : null);
    }
}
