package com.jeepclub.backend.health.core.application.query;

import com.jeepclub.backend.health.api.module.medicalprofile.MedicalProfileOwner;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.repository.MedicalProfileCoverageRepository;
import com.jeepclub.backend.health.core.repository.MedicalProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MedicalProfilePublicQueriesTest {
    @ParameterizedTest
    @EnumSource(MedicalProfileOwner.class)
    void existenceQueryMapsBothOwnerTypesAndPreservesMissingResult(MedicalProfileOwner type) {
        var repository = mock(MedicalProfileRepository.class);
        var query = new HealthMedicalProfileQueryService(repository);
        var domainType = MedicalProfileOwnerType.valueOf(type.name());
        when(repository.existsByOwner(domainType, 7L)).thenReturn(true);
        assertThat(query.existsByOwner(type, 7L)).isTrue();
        assertThat(query.existsByOwner(type, 8L)).isFalse();
        verify(repository).existsByOwner(domainType, 7L);
        verify(repository).existsByOwner(domainType, 8L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void absentOwnerArgumentsDoNotQueryPersistence() {
        var repository = mock(MedicalProfileRepository.class);
        var query = new HealthMedicalProfileQueryService(repository);
        assertThat(query.existsByOwner(null, 7L)).isFalse();
        assertThat(query.existsByOwner(MedicalProfileOwner.USER, null)).isFalse();
        verifyNoInteractions(repository);
    }

    @Test
    void coverageQueryLimitsBatchesAndReturnsOnlyCoveredIds() {
        var repository = mock(MedicalProfileCoverageRepository.class);
        var query = new MedicalProfileCoverageQueryService(repository);
        assertThat(query.findCovered(MedicalProfileOwner.USER, List.of())).isEmpty();
        var batch = LongStream.rangeClosed(1, 500).boxed().toList();
        when(repository.findCovered(MedicalProfileOwner.DEPENDENT, batch)).thenReturn(Set.of(1L, 500L));
        assertThat(query.findCovered(MedicalProfileOwner.DEPENDENT, batch)).containsExactlyInAnyOrder(1L, 500L);
        assertThatThrownBy(() -> query.findCovered(MedicalProfileOwner.USER, LongStream.rangeClosed(1, 501).boxed().toList()))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository).findCovered(MedicalProfileOwner.DEPENDENT, batch);
        verifyNoMoreInteractions(repository);
    }
}
