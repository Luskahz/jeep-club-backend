package com.jeepclub.backend.platform.logging;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SystemLogPersistenceWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemLogPersistenceWorker.class);
    private static final int BATCH_SIZE = 100;

    private final SystemLogServiceAdapter queue;
    private final SystemLogJpaRepository repository;

    @Value("${platform.logging.retention-days:90}")
    private long retentionDays;

    @Scheduled(fixedDelayString = "${platform.logging.persistence-delay-ms:1000}")
    public void persistNextBatch() {
        var events = queue.drain(BATCH_SIZE);
        if (events.isEmpty()) {
            return;
        }

        try {
            repository.saveAll(events.stream().map(SystemLogEntity::new).toList());
        } catch (RuntimeException exception) {
            queue.requeue(events);
            LOGGER.error("system_log persistence_failed batchSize={}", events.size(), exception);
        }
    }

    @Scheduled(
            initialDelayString = "${platform.logging.cleanup-initial-delay-ms:60000}",
            fixedDelayString = "${platform.logging.cleanup-delay-ms:86400000}"
    )
    public void deleteExpiredLogs() {
        try {
            int deleted = repository.deleteOlderThan(java.time.Instant.now()
                    .minus(java.time.Duration.ofDays(retentionDays)));
            if (deleted > 0) {
                LOGGER.info("system_log cleanup deleted={}", deleted);
            }
        } catch (RuntimeException exception) {
            LOGGER.error("system_log cleanup_failed", exception);
        }
    }
}
