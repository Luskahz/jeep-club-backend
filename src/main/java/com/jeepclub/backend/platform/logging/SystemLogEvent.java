package com.jeepclub.backend.platform.logging;

import java.time.Instant;

public record SystemLogEvent(
        Long actorId,
        String action,
        String method,
        String path,
        int status,
        SystemLogOutcome outcome,
        long durationMillis,
        String requestId,
        Instant occurredAt
) {
}
