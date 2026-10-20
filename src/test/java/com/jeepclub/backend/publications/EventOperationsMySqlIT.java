package com.jeepclub.backend.publications;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

/** Repeats all Event financial, transport, rollback and race scenarios on migrated MySQL. */
@Testcontainers
class EventOperationsMySqlIT extends EventOperationsIntegrationTest {
    @org.springframework.beans.factory.annotation.Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @org.springframework.beans.factory.annotation.Autowired com.jeepclub.backend.publications.core.repository.PublicationRepository publications;
    @org.springframework.beans.factory.annotation.Autowired com.jeepclub.backend.publications.infra.persistence.jpa.EventGuestRequestJpaRepository guestRows;
    @org.springframework.beans.factory.annotation.Autowired com.jeepclub.backend.publications.infra.persistence.jpa.EventRegistrationJpaRepository registrationRows;
    @org.springframework.beans.factory.annotation.Autowired javax.sql.DataSource dataSource;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.jeepclub.backend.publications.infra.persistence.adapter.EventCurrentIds auxiliarySnapshots;

    @org.junit.jupiter.api.Test void standardGuestCommandsSerializeWithOnlyTwoPoolConnections() throws Exception {
        var e=freeEvent();member.register(e.getId(),10L,memberOnly());
        var guests=new java.util.ArrayList<com.jeepclub.backend.publications.core.domain.model.EventGuestRequest>();
        for(int i=0;i<4;i++)guests.add(admin.createGuest(e.getId(),99L,guestCpf(e)));
        var pool=dataSource.unwrap(com.zaxxer.hikari.HikariDataSource.class);
        int maximum=pool.getMaximumPoolSize(),minimum=pool.getMinimumIdle();
        var executor=java.util.concurrent.Executors.newFixedThreadPool(4);
        var start=new java.util.concurrent.CountDownLatch(1);
        try {
            pool.setMinimumIdle(0);pool.setMaximumPoolSize(2);pool.getHikariPoolMXBean().softEvictConnections();
            org.mockito.Mockito.clearInvocations(auxiliarySnapshots);
            var futures=guests.stream().map(g -> executor.submit(() -> {
                start.await();
                try {return (Object)admin.allocateGuest(e.getId(),g.id(),101L,null,99L,"Bounded pool allocation");}
                catch(RuntimeException ex) {return (Object)ex;}
            })).toList();
            start.countDown();var results=new java.util.ArrayList<Object>();
            for(var future:futures)results.add(future.get(20,java.util.concurrent.TimeUnit.SECONDS));
            org.assertj.core.api.Assertions.assertThat(results.stream().filter(com.jeepclub.backend.publications.core.domain.model.EventGuestRequest.class::isInstance)).hasSize(2);
            org.assertj.core.api.Assertions.assertThat(results.stream().filter(RuntimeException.class::isInstance)).hasSize(2)
                .allSatisfy(result -> org.assertj.core.api.Assertions.assertThat(result).isInstanceOf(com.jeepclub.backend.publications.core.application.exception.EventOperationException.class));
            org.assertj.core.api.Assertions.assertThat(admin.dashboard(e.getId()).guestsAllocated()).isEqualTo(2);
            org.assertj.core.api.Assertions.assertThat(admin.dashboard(e.getId()).availableSeats()).isZero();
            org.mockito.Mockito.verifyNoInteractions(auxiliarySnapshots);
        } finally {
            start.countDown();pool.setMaximumPoolSize(maximum);pool.setMinimumIdle(minimum);
            pool.getHikariPoolMXBean().softEvictConnections();executor.shutdownNow();
        }
    }

    /** Preload stale ORM/collection snapshots, then prove an actual Event lock
     * wait before committing the competing change. No timing sleeps. */
    Object blockedWithOldSnapshot(Long event,Runnable change,java.util.function.Supplier<?> command) throws Exception {
        var observed=new java.util.concurrent.CompletableFuture<Long>();
        var proceed=new java.util.concurrent.CountDownLatch(1);
        var executor=java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            var pending=executor.submit(() -> {
                try {
                    return tx(() -> {
                        publications.findById(event).orElseThrow().getImages().size();
                        guestRows.findByEventId(event).size();
                        registrationRows.findByEventId(event).forEach(r -> {r.getVehicleIds().size();r.getOccupants().size();r.getUnallocatedDependentIds().size();});
                        observed.complete(jdbc.queryForObject("SELECT CONNECTION_ID()",Long.class));
                        try { if(!proceed.await(20,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("Fixture timeout"); }
                        catch(InterruptedException ex) {Thread.currentThread().interrupt();throw new IllegalStateException(ex);}
                        return command.get();
                    });
                } catch(RuntimeException ex) { return ex; }
            });
            long session=observed.get(20,java.util.concurrent.TimeUnit.SECONDS);
            tx(() -> {
                publications.findByIdForUpdate(event).orElseThrow();change.run();proceed.countDown();
                long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(15);
                boolean waiting=false;
                while(!waiting && System.nanoTime()<deadline) {
                    waiting=jdbc.queryForObject("SELECT COUNT(*) FROM performance_schema.data_lock_waits w JOIN performance_schema.threads t ON t.THREAD_ID=w.REQUESTING_THREAD_ID WHERE t.PROCESSLIST_ID=?",Long.class,session)>0;
                    if(!waiting)Thread.onSpinWait();
                }
                org.assertj.core.api.Assertions.assertThat(waiting).as("Actual Event lock contention").isTrue();
                org.assertj.core.api.Assertions.assertThat(pending.isDone()).isFalse();
                return null;
            });
            return pending.get(20,java.util.concurrent.TimeUnit.SECONDS);
        } finally {proceed.countDown();executor.shutdownNow();}
    }
    @org.junit.jupiter.api.Test void staleGuestSnapshotCannotReserveTheLastSeatTwice() throws Exception {
        var e=freeEvent();member.register(e.getId(),10L,allocation());
        var a=admin.createGuest(e.getId(),99L,guestCpf(e));var b=admin.createGuest(e.getId(),99L,guestCpf(e));
        var result=blockedWithOldSnapshot(e.getId(),() -> admin.allocateGuest(e.getId(),a.id(),101L,null,99L,"First seat"),
            () -> admin.allocateGuest(e.getId(),b.id(),101L,null,99L,"Stale attempt"));
        org.assertj.core.api.Assertions.assertThat(result).isInstanceOf(com.jeepclub.backend.publications.core.application.exception.EventOperationException.class);
        org.assertj.core.api.Assertions.assertThat(admin.dashboard(e.getId()).guestsAllocated()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(guestState(e.getId(),b.id()).transportStatus()).isEqualTo(com.jeepclub.backend.publications.core.domain.model.EventGuestRequest.TransportStatus.STANDBY);
    }
    @org.junit.jupiter.api.Test void staleRegistrationSnapshotCannotAllocateAfterDriverCancellation() throws Exception {
        var e=freeEvent();member.register(e.getId(),10L,allocation());var g=admin.createGuest(e.getId(),99L,guestCpf(e));
        var result=blockedWithOldSnapshot(e.getId(),() -> member.cancel(e.getId(),10L),
            () -> admin.allocateGuest(e.getId(),g.id(),101L,null,99L,"Stale registration"));
        org.assertj.core.api.Assertions.assertThat(result).isInstanceOf(com.jeepclub.backend.publications.core.application.exception.EventOperationException.class);
        org.assertj.core.api.Assertions.assertThat(guestState(e.getId(),g.id()).vehicleId()).isNull();
        org.assertj.core.api.Assertions.assertThat(member.mine(e.getId(),10L).status()).isEqualTo(com.jeepclub.backend.publications.core.domain.model.EventRegistration.Status.CANCELLED);
    }
    @org.junit.jupiter.api.Test void staleGuestAndOfferSnapshotCannotSelectAfterGuestCancellation() throws Exception {
        var e=freeEvent();member.register(e.getId(),10L,allocation());var g=admin.createGuest(e.getId(),99L,guestCpf(e));var offer=member.respond(e.getId(),g.id(),10L,101L,true);
        var result=blockedWithOldSnapshot(e.getId(),() -> admin.cancelGuest(e.getId(),g.id(),99L,"Cancelled"),() -> admin.select(e.getId(),offer.id(),99L));
        org.assertj.core.api.Assertions.assertThat(result).isInstanceOf(com.jeepclub.backend.publications.core.application.exception.EventOperationException.class);
        org.assertj.core.api.Assertions.assertThat(guestState(e.getId(),g.id()).status()).isEqualTo(com.jeepclub.backend.publications.core.domain.model.EventGuestRequest.Status.CANCELLED);
        org.assertj.core.api.Assertions.assertThat(admin.dashboard(e.getId()).guestsAllocated()).isZero();
    }
    @org.junit.jupiter.api.Test void staleEventSnapshotCannotAllocateAfterEventCancellation() throws Exception {
        var e=freeEvent();member.register(e.getId(),10L,allocation());var g=admin.createGuest(e.getId(),99L,guestCpf(e));
        var result=blockedWithOldSnapshot(e.getId(),() -> admin.cancel(e.getId(),99L),
            () -> admin.allocateGuest(e.getId(),g.id(),101L,null,99L,"Stale Event state"));
        org.assertj.core.api.Assertions.assertThat(result).isInstanceOf(com.jeepclub.backend.publications.core.application.exception.EventOperationException.class);
        org.assertj.core.api.Assertions.assertThat(guestState(e.getId(),g.id()).vehicleId()).isNull();
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
