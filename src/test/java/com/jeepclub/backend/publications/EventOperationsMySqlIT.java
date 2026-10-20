package com.jeepclub.backend.publications;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

/** Repeats all Event financial, transport, rollback and race scenarios on migrated MySQL. */
@Testcontainers
class EventOperationsMySqlIT extends EventOperationsIntegrationTest {
    @Override long currentDatabaseSession() {
        return jdbc.queryForObject("SELECT CONNECTION_ID()", Long.class);
    }

    @Override boolean isDatabaseSessionWaitingForLock(long session) {
        return jdbc.queryForObject("""
            SELECT COUNT(*) FROM performance_schema.data_lock_waits w
            JOIN performance_schema.threads t ON t.THREAD_ID = w.REQUESTING_THREAD_ID
            WHERE t.PROCESSLIST_ID = ?
            """, Long.class, session) > 0;
    }

    @Container static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.46")
        .withUsername("root").withPassword("synthetic-event-test-only");

    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> true);
    }

    @org.junit.jupiter.api.Test
    void oldRepeatableReadSnapshotCannotOverwriteConfirmedMoneyOrFinishedAudit() throws Exception {
        var event = event(false); member.register(event.getId(), 10L, java.util.List.of());
        var payment = receipt(event.getId(), 10L);
        var cycleId = tx(() -> contexts.findByEvent(event.getId()).get(0).cycleId());
        var observed = new java.util.concurrent.CountDownLatch(1);
        var changed = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            var cancellation = executor.submit(() -> tx(() -> {
                financial.findByEvent(event.getId()); // Cache mutable entities and establish the old RR snapshot.
                observed.countDown();
                try {
                    if (!changed.await(20, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Fixture timeout");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); throw new IllegalStateException(ex);
                }
                return admin.cancel(event.getId(), 99L);
            }));
            org.assertj.core.api.Assertions.assertThat(observed.await(20, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            paymentAdmin.confirm(payment, 99L);
            cycleAdmin.finish(cycleId, 98L);
            changed.countDown();
            cancellation.get(20, java.util.concurrent.TimeUnit.SECONDS);
            org.assertj.core.api.Assertions.assertThat(financial.findByEvent(event.getId()).get(0).effectiveStatus()).isEqualTo("PAID");
            org.assertj.core.api.Assertions.assertThat(cycleAdmin.findById(cycleId).finishedByUserId()).isEqualTo(98L);
            org.assertj.core.api.Assertions.assertThat(tx(() -> refunds.findActiveByMemberPaymentId(payment))).isPresent();
        } finally {
            changed.countDown(); executor.shutdownNow();
        }
    }
}
