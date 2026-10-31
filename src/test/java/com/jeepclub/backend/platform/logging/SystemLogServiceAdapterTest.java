package com.jeepclub.backend.platform.logging;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SystemLogServiceAdapterTest {
    private java.util.List<ch.qos.logback.classic.spi.ILoggingEvent> capture(Runnable work) {
        var logger=(ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger(SystemLogServiceAdapter.class);
        var previous=logger.getLevel();
        var appender=new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start();logger.addAppender(appender);logger.setLevel(ch.qos.logback.classic.Level.INFO);
        try { work.run();return java.util.List.copyOf(appender.list); }
        finally { logger.detachAppender(appender);appender.stop();logger.setLevel(previous); }
    }
    private SystemLogEvent event(String action) {
        return new SystemLogEvent(42L, action, "INTERNAL", "/synthetic", 200,
                SystemLogOutcome.SUCCESS, 0, "synthetic-log", Instant.EPOCH);
    }
    @Test void fullQueueDropsOnlyNewEventsAndNeverDisplacesPendingAudit() {
        var service = new SystemLogServiceAdapter(org.mockito.Mockito.mock(SystemLogJpaRepository.class));
        var retained = event("RETAINED");
        var logged=capture(() -> {
            service.requeue(java.util.Collections.nCopies(5000, retained));
            service.record(event("DROPPED"));
            service.requeue(java.util.List.of(event("RETRY_DROPPED")));
        });
        assertThat(service.drain(5002)).hasSize(5000).allSatisfy(e -> assertThat(e).isSameAs(retained));
        assertThat(service.drain(1)).isEmpty();
        assertThat(logged.stream().filter(e -> e.getLevel()==ch.qos.logback.classic.Level.WARN)
                .map(ch.qos.logback.classic.spi.ILoggingEvent::getFormattedMessage).toList())
                .hasSize(2).anySatisfy(message -> assertThat(message).contains("dropped reason=queue_full action=DROPPED"))
                .anySatisfy(message -> assertThat(message).contains("retry_dropped reason=queue_full action=RETRY_DROPPED"));
    }
    @Test void drainHonorsBatchLimitAndRequeuePreservesOrder() {
        var service = new SystemLogServiceAdapter(org.mockito.Mockito.mock(SystemLogJpaRepository.class));
        var first = event("FIRST"); var second = event("SECOND"); var third = event("THIRD");
        service.requeue(java.util.List.of(first,second,third));
        assertThat(service.drain(0)).isEmpty();
        assertThat(service.drain(2)).containsExactly(first,second);
        assertThat(service.drain(2)).containsExactly(third);
    }
    @Test void requiredAndTransactionalAuditPropagatePersistenceFailure() {
        var repository = org.mockito.Mockito.mock(SystemLogJpaRepository.class);
        var failure = new org.springframework.dao.DataAccessResourceFailureException("synthetic failure");
        org.mockito.Mockito.doThrow(failure).when(repository).saveAndFlush(org.mockito.ArgumentMatchers.any());
        var service = new SystemLogServiceAdapter(repository);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.recordRequired(event("REQUIRED"))).isSameAs(failure);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.recordInTransaction(event("TRANSACTIONAL"))).isSameAs(failure);
        assertThat(service.drain(10)).isEmpty();
    }

    @Test
    void keepsExplicitPersistentSystemLogEventsAvailableToWorker() {
        var service = new SystemLogServiceAdapter(org.mockito.Mockito.mock(SystemLogJpaRepository.class));
        var event = new SystemLogEvent(
                42L,
                "SECURITY_CONFIGURATION_CHANGED",
                "INTERNAL",
                "/system-log",
                200,
                SystemLogOutcome.SUCCESS,
                0,
                "intentional-event-1",
                Instant.parse("2026-09-13T00:00:00Z")
        );

        var logged=capture(() -> service.record(event));

        assertThat(service.drain(10)).containsExactly(event);
        assertThat(logged).singleElement().satisfies(line -> {
            assertThat(line.getLevel()).isEqualTo(ch.qos.logback.classic.Level.INFO);
            assertThat(line.getFormattedMessage()).contains("action=SECURITY_CONFIGURATION_CHANGED","status=200",
                    "actorId=42","requestId=intentional-event-1");
        });
    }
}
