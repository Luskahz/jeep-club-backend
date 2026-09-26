package com.jeepclub.backend.dependents.core.application.query;

import com.jeepclub.backend.dependents.core.repository.DependentRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DependentsQueryServiceTest {
    private final DependentRepository repository = mock(DependentRepository.class);
    private final DependentsQueryService query = new DependentsQueryService(repository);

    @Test
    void absentIdentifiersAndEmptyBatchesNeverReachPersistence() {
        assertThat(query.existsById(null)).isFalse();
        assertThat(query.existsActiveById(null)).isFalse();
        assertThat(query.isActiveDependentOfUser(null, 1L)).isFalse();
        assertThat(query.isActiveDependentOfUser(1L, null)).isFalse();
        assertThat(query.findActiveDependentIdsByIds(null)).isEmpty();
        assertThat(query.findActiveDependentIdsByIds(List.of())).isEmpty();
        verifyNoInteractions(repository);
    }
}
