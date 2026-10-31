package com.jeepclub.backend.lifecycle;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

/** Runs the same real lifecycle, retention, history, rollback and race cases on Flyway-managed MySQL. */
@Testcontainers
class PrincipalLifecycleMySqlIT extends PrincipalLifecycleIntegrationTest {
    @org.springframework.beans.factory.annotation.Autowired
    com.jeepclub.backend.iam.identity.api.module.UserIdentityLock identityLocks;
    @org.springframework.beans.factory.annotation.Autowired
    com.jeepclub.backend.dependents.api.module.DependentLifecycleLock dependentLocks;
    @org.springframework.beans.factory.annotation.Autowired
    com.jeepclub.backend.health.core.application.service.medicalprofile.AdminMedicalProfileService health;

    private com.jeepclub.backend.dependents.core.application.result.DependentResult createDependent(Long owner) {
        return dependents.create("Concurrent dependent",String.valueOf(IDS.incrementAndGet()),java.time.LocalDate.of(2010,1,1),
                com.jeepclub.backend.dependents.core.domain.enums.RelationshipType.CHILD,null,owner);
    }

    private static com.jeepclub.backend.health.core.application.command.UpsertMedicalProfileCommand clinicalData() {
        return new com.jeepclub.backend.health.core.application.command.UpsertMedicalProfileCommand(
                com.jeepclub.backend.health.core.domain.enums.BloodType.UNKNOWN,null,null,null,null,null,null,null,null,null,null);
    }

    // The holder owns the parent row before the contender starts. No sleeps or timing-based outcomes.
    private void orderedRace(Runnable lock,Runnable holderAction,Runnable contender) throws Exception {
        var locked=new java.util.concurrent.CountDownLatch(1);
        var contenderStarted=new java.util.concurrent.CountDownLatch(1);
        var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var holder=executor.submit(() -> tx(() -> {
                lock.run();locked.countDown();await(contenderStarted);awaitDatabaseWait();holderAction.run();return null;
            }));
            await(locked);
            var other=executor.submit(() -> { contenderStarted.countDown();contender.run(); });
            holder.get(30,java.util.concurrent.TimeUnit.SECONDS);
            other.get(30,java.util.concurrent.TimeUnit.SECONDS);
        } finally { locked.countDown();contenderStarted.countDown();executor.shutdownNow(); }
    }

    private static void await(java.util.concurrent.CountDownLatch latch) {
        try { org.assertj.core.api.Assertions.assertThat(latch.await(20,java.util.concurrent.TimeUnit.SECONDS)).isTrue(); }
        catch(InterruptedException e) { Thread.currentThread().interrupt();throw new IllegalStateException(e); }
    }

    private void awaitDatabaseWait() {
        long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(20);
        while(System.nanoTime()<deadline) {
            Integer waits=jdbc.queryForObject("SELECT COUNT(*) FROM performance_schema.data_lock_waits w "
                    +"JOIN information_schema.innodb_trx b ON b.trx_id=w.blocking_engine_transaction_id "
                    +"WHERE b.trx_mysql_thread_id=CONNECTION_ID()",Integer.class);
            if(waits!=null && waits>0)return;
            Thread.onSpinWait();
        }
        throw new AssertionError("Contender never reached a real MySQL lock wait on the holder transaction");
    }

    @org.junit.jupiter.api.RepeatedTest(3)
    void dependentCreationAndUserDeletionSerializeInBothOrders() throws Exception {
        Long deleted=user(true);
        orderedRace(() -> identityLocks.byId(deleted).orElseThrow(),
                () -> identity.delete(deleted,deleted,CREATED.plusSeconds(60)),
                () -> org.assertj.core.api.Assertions.assertThatThrownBy(() -> createDependent(deleted))
                        .isInstanceOf(com.jeepclub.backend.dependents.core.application.exception.DependentOwnerNotFoundException.class));
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dependents_dependent WHERE user_id=?",Long.class,deleted)).isZero();
        Long retained=user(true);
        orderedRace(() -> identityLocks.byId(retained).orElseThrow(),() -> createDependent(retained),
                () -> org.assertj.core.api.Assertions.assertThatThrownBy(() -> identity.delete(retained,retained,CREATED.plusSeconds(60)))
                        .isInstanceOf(com.jeepclub.backend.iam.identity.api.module.exception.UserDeletionConflictException.class));
        org.assertj.core.api.Assertions.assertThat(users.findById(retained)).isPresent();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType.class)
    void profileCreationAndOwnerDeletionSerializeInBothOrders(com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType type) throws Exception {
        for(boolean deleteFirst:new boolean[]{true,false}) {
            Long user=user(true);
            Long owner=type==com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType.USER?user:createDependent(user).id();
            Runnable lock=type==com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType.USER
                    ?() -> identityLocks.byId(owner).orElseThrow():() -> dependentLocks.byId(owner).orElseThrow();
            Runnable delete=type==com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType.USER
                    ?() -> identity.delete(owner,user,CREATED.plusSeconds(60)):() -> dependents.delete(owner,user,user);
            Runnable create=() -> health.upsertByOwner(type,owner,clinicalData());
            // Dependent deletion must acquire its Identity history locks before its row.
            Runnable orderedLock=() -> { identityLocks.byId(user).orElseThrow();lock.run(); };
            if(deleteFirst) orderedRace(orderedLock,delete,
                    () -> org.assertj.core.api.Assertions.assertThatThrownBy(create::run)
                            .isInstanceOf(com.jeepclub.backend.health.core.application.exceptions.MedicalProfileOwnerNotFoundException.class));
            else orderedRace(orderedLock,create,
                    () -> org.assertj.core.api.Assertions.assertThatThrownBy(delete::run)
                            .isInstanceOf(type==com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType.USER
                                    ?com.jeepclub.backend.iam.identity.api.module.exception.UserDeletionConflictException.class
                                    :com.jeepclub.backend.dependents.core.domain.exception.DependentLifecycleConflictException.class));
            org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM medical_profiles WHERE owner_type=? AND owner_id=?",
                    Long.class,type.name(),owner)).isEqualTo(deleteFirst?0L:1L);
        }
    }

    @org.junit.jupiter.api.RepeatedTest(3)
    void simultaneousDependentAndUserDeletionRetainHistoryWithoutDeadlock() throws Exception {
        Long owner=user(true),actor=user(true);Long dependent=createDependent(owner).id();
        orderedRace(() -> { identityLocks.byId(owner).orElseThrow();identityLocks.byId(actor).orElseThrow(); },
                () -> dependents.delete(dependent,owner,actor),
                () -> org.assertj.core.api.Assertions.assertThatThrownBy(() -> identity.delete(owner,owner,CREATED.plusSeconds(60)))
                        .isInstanceOf(com.jeepclub.backend.iam.identity.api.module.exception.UserDeletionConflictException.class));
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dependents_dependent_history WHERE dependent_id=?",Long.class,dependent)).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update("DELETE FROM identity_users WHERE id=?",actor))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @org.junit.jupiter.api.Test
    void rolledBackCreationsReleaseLocksAndDoNotRetainReferences() {
        Long owner=user(true);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> tx(() -> {
            createDependent(owner);
            health.upsertByOwner(com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType.USER,owner,clinicalData());
            throw new IllegalStateException("synthetic rollback");
        })).isInstanceOf(IllegalStateException.class);
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dependents_dependent WHERE user_id=?",Long.class,owner)).isZero();
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM medical_profiles WHERE owner_type='USER' AND owner_id=?",Long.class,owner)).isZero();
        identity.delete(owner,owner,CREATED.plusSeconds(60));
        org.assertj.core.api.Assertions.assertThat(users.findById(owner)).isEmpty();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType.class)
    void medicalDeletionAndActorDeletionRetainEvidenceWithoutDeadlock(com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType type) throws Exception {
        Long actor=user(true),responsible=user(true);
        Long owner=type==com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType.USER
                ?responsible:createDependent(responsible).id();
        Long profile=health.upsertByOwner(type,owner,clinicalData()).getId();
        orderedRace(() -> identityLocks.byId(actor).orElseThrow(),() -> health.deleteById(profile,actor),
                () -> org.assertj.core.api.Assertions.assertThatThrownBy(() -> identity.delete(actor,actor,CREATED.plusSeconds(60)))
                        .isInstanceOf(com.jeepclub.backend.iam.identity.api.module.exception.UserDeletionConflictException.class));
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM medical_profiles WHERE id=?",Long.class,profile)).isZero();
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM medical_profiles_history WHERE medical_profile_id=? AND deleted_by_user_id=?",Long.class,profile,actor)).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(users.findById(actor)).isPresent();
    }

    @org.junit.jupiter.api.Test
    void profileDeletionRetainsOwnerAndActorEvenWhenOwnerIsArchived() {
        Long owner=user(true),actor=user(true);
        Long dependent=createDependent(owner).id();
        Long profile=health.upsertByOwner(com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType.DEPENDENT,dependent,clinicalData()).getId();
        dependents.archive(dependent,owner,actor);identity.archive(owner,actor,CREATED.plusSeconds(60));
        health.deleteById(profile,actor);
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM medical_profiles_history WHERE medical_profile_id=?",Long.class,profile)).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update("DELETE FROM dependents_dependent WHERE id=?",dependent)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update("DELETE FROM identity_users WHERE id=?",actor)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
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
