package com.jeepclub.backend.lifecycle;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

/** Runs the same real lifecycle, retention, history, rollback and race cases on Flyway-managed MySQL. */
@Testcontainers
class PrincipalLifecycleMySqlIT extends PrincipalLifecycleIntegrationTest {
    @Container static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.46")
            .withUsername("root").withPassword("synthetic-lifecycle-test-only");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username",MYSQL::getUsername);
        registry.add("spring.datasource.password",MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name",() -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.database-platform",() -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto",() -> "validate");
        registry.add("spring.flyway.enabled",() -> true);
    }

    @org.junit.jupiter.api.Test
    void currentUserLockDiscardsAnOldRepeatableReadOrmSnapshot() throws Exception {
        Long id=user(false);
        var observed=new java.util.concurrent.CountDownLatch(1);
        var changed=new java.util.concurrent.CountDownLatch(1);
        var executor=java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            var oldReader=executor.submit(() -> tx(() -> {
                users.findById(id).orElseThrow();
                observed.countDown();
                try { if(!changed.await(20,java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Fixture timeout"); }
                catch(InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException(exception); }
                return identity.archive(id,id,CREATED.plusSeconds(120));
            }));
            org.assertj.core.api.Assertions.assertThat(observed.await(20,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            identity.archive(id,id,CREATED.plusSeconds(60)); changed.countDown();
            org.assertj.core.api.Assertions.assertThat(oldReader.get(20,java.util.concurrent.TimeUnit.SECONDS).archivedAt()).isEqualTo(CREATED.plusSeconds(60));
            org.assertj.core.api.Assertions.assertThat(users.findById(id).orElseThrow().getArchivedAt()).isEqualTo(CREATED.plusSeconds(60));
        } finally { changed.countDown(); executor.shutdownNow(); }
    }

    @org.junit.jupiter.api.Test
    void pendingDeletionAndConcurrentAdministrativeToolCreationCannotLeaveAnOrphan() throws Exception {
        Long id=user(true);
        var locked=new java.util.concurrent.CountDownLatch(1);
        var release=new java.util.concurrent.CountDownLatch(1);
        var writerStarted=new java.util.concurrent.CountDownLatch(1);
        var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var deletion=executor.submit(() -> tx(() -> {
                users.findByIdForUpdate(id).orElseThrow(); locked.countDown();
                try { if(!release.await(20,java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Fixture timeout"); }
                catch(InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException(exception); }
                identity.delete(id,id,CREATED.plusSeconds(60)); return null;
            }));
            org.assertj.core.api.Assertions.assertThat(locked.await(20,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            var writer=executor.submit(() -> {
                writerStarted.countDown();
                try { tools.createToolForUser(id,"Concurrent synthetic tool",null); return "CREATED"; }
                catch(com.jeepclub.backend.tools.core.application.exception.ToolOwnerNotFoundException
                        | com.jeepclub.backend.tools.core.application.exception.ToolOwnerDisabledException exception) { return "REJECTED"; }
            });
            org.assertj.core.api.Assertions.assertThat(writerStarted.await(20,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            release.countDown(); deletion.get(20,java.util.concurrent.TimeUnit.SECONDS);
            org.assertj.core.api.Assertions.assertThat(writer.get(20,java.util.concurrent.TimeUnit.SECONDS)).isEqualTo("REJECTED");
            org.assertj.core.api.Assertions.assertThat(users.findById(id)).isEmpty();
            org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tools WHERE user_id=?",Long.class,id)).isZero();
        } finally { release.countDown(); executor.shutdownNow(); }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"Dependent","Tool","Vehicle","Role","ChargeDefinition"})
    void repeatedConcurrentArchivalIsIdempotentAcrossTheOtherFiveAggregates(String aggregate) throws Exception {
        Long owner=user(false);
        Runnable archive;
        java.util.function.Supplier<String> state;
        switch(aggregate) {
            case "Dependent" -> {
                var id=dependents.create("Race dependent",String.valueOf(IDS.incrementAndGet()),java.time.LocalDate.of(2010,1,1),
                        com.jeepclub.backend.dependents.core.domain.enums.RelationshipType.CHILD,null,owner).id();
                archive=() -> dependents.archive(id,owner,owner);
                state=() -> dependents.findById(id,owner).status().name();
            }
            case "Tool" -> {
                var id=tools.createToolForUser(owner,"Race tool",null).getId();
                archive=() -> tools.archiveTool(id,owner);
                state=() -> tools.getToolDetails(id).getStatus().name();
            }
            case "Vehicle" -> {
                long unique=IDS.incrementAndGet();
                var id=vehicles.createForOwner("Race",null,"RAC"+String.format("%04d",unique%10000),String.valueOf(unique),
                        "Jeep","Test",2020,2020,"Blue",5,com.jeepclub.backend.vehicles.core.domain.enums.FuelType.DIESEL,2d,false,owner).getId();
                archive=() -> vehicles.archive(id,owner);
                state=() -> vehicles.findById(id).getStatus().name();
            }
            case "Role" -> {
                var id=roles.createRole("Race "+java.util.UUID.randomUUID(),null).role().getId();
                archive=() -> roles.archiveRole(id,owner);
                state=() -> roles.findRoleById(id).role().getStatus().name();
            }
            default -> {
                var id=definition();
                archive=() -> definitions.archive(id,owner);
                state=() -> definitions.findById(id).status().name();
            }
        }
        var start=new java.util.concurrent.CountDownLatch(1);
        var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<Void> command=() -> {
                org.assertj.core.api.Assertions.assertThat(start.await(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                archive.run(); return null;
            };
            var a=executor.submit(command); var b=executor.submit(command); start.countDown();
            a.get(20,java.util.concurrent.TimeUnit.SECONDS); b.get(20,java.util.concurrent.TimeUnit.SECONDS);
            org.assertj.core.api.Assertions.assertThat(state.get()).isEqualTo("ARCHIVED");
        } finally { start.countDown(); executor.shutdownNow(); }
    }
}
