package com.jeepclub.backend.dependents.infra.integration.health;

import com.jeepclub.backend.dependents.infra.persistence.jpa.DependentJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthDependentOwnershipAccessAdapterTest {

    @Mock
    private DependentJpaRepository dependentJpaRepository;

    @Test
    void rejectsNullIdentifiersWithoutQueryingRepository() {
        HealthDependentOwnershipAccessAdapter adapter =
                new HealthDependentOwnershipAccessAdapter(dependentJpaRepository);

        assertThat(adapter.belongsToUser(null, 1L)).isFalse();
        assertThat(adapter.belongsToUser(10L, null)).isFalse();

        verifyNoInteractions(dependentJpaRepository);
    }

    @Test
    void returnsRepositoryOwnershipResult() {
        HealthDependentOwnershipAccessAdapter adapter =
                new HealthDependentOwnershipAccessAdapter(dependentJpaRepository);

        when(dependentJpaRepository.existsByIdAndUserId(10L, 1L)).thenReturn(true);
        when(dependentJpaRepository.existsByIdAndSocioId(10L, 2L)).thenReturn(false);

        assertThat(adapter.belongsToUser(10L, 1L)).isTrue();
        assertThat(adapter.belongsToUser(10L, 2L)).isFalse();
        verify(dependentJpaRepository).existsByIdAndSocioId(10L, 1L);
        verify(dependentJpaRepository).existsByIdAndSocioId(10L, 2L);
    }
}
