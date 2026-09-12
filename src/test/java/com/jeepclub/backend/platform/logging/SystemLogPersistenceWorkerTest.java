package com.jeepclub.backend.platform.logging;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemLogPersistenceWorkerTest {

    @Test
    void persistsDrainedEventsAsOneBatch() {
        SystemLogServiceAdapter queue = mock(SystemLogServiceAdapter.class);
        SystemLogJpaRepository repository = mock(SystemLogJpaRepository.class);
        SystemLogEvent event = new SystemLogEvent(
                7L, "DELETE /vehicles/{id}", "DELETE", "/vehicles/42",
                204, SystemLogOutcome.SUCCESS, 12, "request-1", Instant.EPOCH
        );
        when(queue.drain(100)).thenReturn(List.of(event));

        new SystemLogPersistenceWorker(queue, repository).persistNextBatch();

        verify(repository).saveAll(argThat(entities -> entities.iterator().hasNext()));
    }
}
