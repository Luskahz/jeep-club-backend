package com.jeepclub.backend.billing.infra.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
class BillingRoleLifecycleMySqlIT extends BillingRoleLifecycleIntegrationTest {
    @Container static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.46")
            .withUsername("root").withPassword("synthetic-test-only");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> true);
    }
    @org.springframework.beans.factory.annotation.Autowired com.jeepclub.backend.iam.authorization.core.repository.RoleRepository roleRepository;
    @org.springframework.beans.factory.annotation.Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    @org.junit.jupiter.api.Test void assignmentWaitsForRoleLockAndRejectsDeactivatedRole() throws Exception {
        var tx = new org.springframework.transaction.support.TransactionTemplate(manager);
        var ids = tx.execute(status -> {
            var now = java.time.Instant.now();
            var definition = definitions.save(com.jeepclub.backend.billing.core.domain.model.ChargeDefinition.create(
                "concurrent role lock", null, java.math.BigDecimal.TEN,
                com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType.ONE_TIME, true,
                com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy.AFTER_DUE_DATE, null, now));
            var role = roles.save(new com.jeepclub.backend.iam.authorization.infra.persistence.entity.RoleEntity(null,
                "concurrent role lock", null, com.jeepclub.backend.iam.authorization.core.domain.enums.RoleKind.CUSTOM,
                com.jeepclub.backend.iam.authorization.core.domain.enums.RoleStatus.ACTIVE, now, null, null));
            return java.util.List.of(definition.getId(), role.getId());
        });
        var locked = new java.util.concurrent.CountDownLatch(1);
        var started = new java.util.concurrent.CountDownLatch(1);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var holder = pool.submit(() -> tx.executeWithoutResult(status -> {
                var role = roleRepository.findByIdForUpdate(ids.get(1)).orElseThrow();
                Long connection = jdbc.queryForObject("SELECT CONNECTION_ID()", Long.class);
                locked.countDown();
                try { if (!started.await(20, java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("contender did not start"); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
                long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(20);
                boolean waited = false;
                while (System.nanoTime() < deadline) {
                    Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM performance_schema.data_lock_waits w JOIN performance_schema.threads b ON b.thread_id=w.blocking_thread_id JOIN performance_schema.data_locks l ON l.engine_lock_id=w.blocking_engine_lock_id WHERE b.processlist_id=? AND l.object_name='authorization_roles'", Integer.class, connection);
                    if (count != null && count > 0) { waited = true; break; }
                    Thread.onSpinWait();
                }
                org.assertj.core.api.Assertions.assertThat(waited).as("wait on role row").isTrue();
                role.deactivate(java.time.Instant.now());
                roleRepository.save(role);
            }));
            org.assertj.core.api.Assertions.assertThat(locked.await(20, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            var contender = pool.submit(() -> {
                started.countDown();
                org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.assignToRole(ids.get(0), ids.get(1)))
                    .isInstanceOf(com.jeepclub.backend.billing.core.application.exception.assignment.BillingAssignmentTargetNotFoundException.class);
            });
            holder.get(30, java.util.concurrent.TimeUnit.SECONDS);
            contender.get(30, java.util.concurrent.TimeUnit.SECONDS);
            org.assertj.core.api.Assertions.assertThat((Boolean) tx.execute(status -> assignments.existsRoleAssignmentByChargeDefinitionIdAndRoleId(ids.get(0), ids.get(1)))).isFalse();
        } finally { started.countDown(); pool.shutdownNow(); }
    }
}
