package com.jeepclub.backend.platform.logging;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Service
@RequiredArgsConstructor
public class SystemLogServiceAdapter implements SystemLogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemLogServiceAdapter.class);
    private static final int QUEUE_CAPACITY = 5_000;
    private final BlockingQueue<SystemLogEvent> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

    @Override
    public void record(SystemLogEvent event) {
        LOGGER.info(
                "system_log action={} method={} path={} status={} outcome={} actorId={} durationMs={} requestId={}",
                event.action(), event.method(), event.path(), event.status(), event.outcome(),
                event.actorId(), event.durationMillis(), event.requestId()
        );
        if (!queue.offer(event)) {
            LOGGER.warn("system_log dropped reason=queue_full action={} requestId={}",
                    event.action(), event.requestId());
        }
    }

    List<SystemLogEvent> drain(int batchSize) {
        List<SystemLogEvent> batch = new ArrayList<>(batchSize);
        queue.drainTo(batch, batchSize);
        return batch;
    }

    void requeue(List<SystemLogEvent> events) {
        events.forEach(event -> {
            if (!queue.offer(event)) {
                LOGGER.warn("system_log retry_dropped reason=queue_full action={} requestId={}",
                        event.action(), event.requestId());
            }
        });
    }
}
