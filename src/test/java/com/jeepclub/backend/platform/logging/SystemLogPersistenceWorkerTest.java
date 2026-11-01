package com.jeepclub.backend.platform.logging;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemLogPersistenceWorkerTest {
    @Test void failedBatchReturnsToRealQueueAndNextExplicitWorkerPassPersistsIt() {
        var repository = mock(SystemLogJpaRepository.class);
        var queue = new SystemLogServiceAdapter(repository);
        var event = new SystemLogEvent(7L,"SYNTHETIC_RETRY","POST","/synthetic",200,SystemLogOutcome.SUCCESS,0,"request",Instant.EPOCH);
        queue.requeue(List.of(event));
        org.mockito.Mockito.doThrow(new org.springframework.dao.DataAccessResourceFailureException("synthetic failure"))
                .doReturn(List.of()).when(repository).saveAll(org.mockito.ArgumentMatchers.any());
        var worker = new SystemLogPersistenceWorker(queue,repository);
        worker.persistNextBatch();
        worker.persistNextBatch();
        verify(repository, org.mockito.Mockito.times(2)).saveAll(argThat(entities -> {
            var list = new java.util.ArrayList<SystemLogEntity>();
            entities.forEach(list::add);
            return list.size() == 1 && list.get(0).getAction().equals(event.action())
                    && list.get(0).getOccurredAt().equals(event.occurredAt());
        }));
        org.assertj.core.api.Assertions.assertThat(queue.drain(10)).isEmpty();
        worker.persistNextBatch();
        org.mockito.Mockito.verifyNoMoreInteractions(repository);
    }

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
