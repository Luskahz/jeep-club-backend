package com.jeepclub.backend.platform.logging;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SystemLogServiceAdapterTest {

    @Test
    void keepsExplicitPersistentSystemLogEventsAvailableToWorker() {
        var service = new SystemLogServiceAdapter();
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

        service.record(event);

        assertThat(service.drain(10)).containsExactly(event);
    }
}
