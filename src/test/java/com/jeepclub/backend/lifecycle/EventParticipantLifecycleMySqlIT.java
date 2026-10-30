package com.jeepclub.backend.lifecycle;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.mysql.MySQLContainer;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class EventParticipantLifecycleMySqlIT extends EventParticipantLifecycleIntegrationTest {
    @Container static final MySQLContainer MYSQL=new MySQLContainer("mysql:8.0.46")
        .withUsername("root").withPassword("synthetic-event-lifecycle-only");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",MYSQL::getJdbcUrl);r.add("spring.datasource.username",MYSQL::getUsername);
        r.add("spring.datasource.password",MYSQL::getPassword);r.add("spring.datasource.driver-class-name",() -> "com.mysql.cj.jdbc.Driver");
        r.add("spring.jpa.database-platform",() -> "org.hibernate.dialect.MySQLDialect");
        r.add("spring.jpa.hibernate.ddl-auto",() -> "validate");r.add("spring.flyway.enabled",() -> true);
    }
    static void await(CountDownLatch latch) {
        try {assertThat(latch.await(20,TimeUnit.SECONDS)).isTrue();}
        catch(InterruptedException ex) {Thread.currentThread().interrupt();throw new IllegalStateException(ex);}
    }
    void actualWait(long connection) {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);
        boolean waiting=false;
        while(!waiting && System.nanoTime()<deadline) {
            waiting=jdbc.queryForObject("SELECT COUNT(*) FROM performance_schema.data_lock_waits w JOIN performance_schema.threads t ON t.THREAD_ID=w.REQUESTING_THREAD_ID WHERE t.PROCESSLIST_ID=?",Long.class,connection)>0;
            Thread.onSpinWait();
        }
        assertThat(waiting).as("Observed real InnoDB lock wait").isTrue();
    }
    @Test void committedRegistrationWinsAgainstWaitingLifecycleEvenWithOldRepeatableReadSnapshot() throws Exception {
        var e=event();var established=new CountDownLatch(1);var proceed=new CountDownLatch(1);
        var connection=new CompletableFuture<Long>();var pool=Executors.newSingleThreadExecutor();
        try {
            var lifecycle=pool.submit(() -> {
                try {return transactions.execute(s -> {
                    jdbc.queryForObject("select count(*) from event_registrations where event_id=?",Long.class,e.getId());
                    connection.complete(jdbc.queryForObject("select connection_id()",Long.class));established.countDown();await(proceed);
                    transition(false,true);return "ARCHIVED";
                });} catch(RuntimeException ex) {return ex;}
            });
            await(established);
            transactions.execute(s -> {member.register(e.getId(),owner,allocation());proceed.countDown();actualWait(connection.join());return null;});
            assertThat(lifecycle.get(20,TimeUnit.SECONDS))
                .isInstanceOf(com.jeepclub.backend.vehicles.core.domain.exception.VehicleLifecycleConflictException.class);
            assertThat(admin.dashboard(e.getId()).totalCapacity()).isEqualTo(5);
        } finally {proceed.countDown();pool.shutdownNow();}
    }
    @Test void lifecycleWinsAgainstWaitingRegistrationWithoutCreatingParticipation() throws Exception {
        var e=event();var proceed=new CountDownLatch(1);var connection=new CompletableFuture<Long>();
        var pool=Executors.newSingleThreadExecutor();
        try {
            var registration=pool.submit(() -> {
                try {return transactions.execute(s -> {
                    connection.complete(jdbc.queryForObject("select connection_id()",Long.class));await(proceed);
                    return member.register(e.getId(),owner,allocation());
                });} catch(RuntimeException ex) {return ex;}
            });
            transactions.execute(s -> {transition(true,false);proceed.countDown();actualWait(connection.join());return null;});
            assertThat(registration.get(20,TimeUnit.SECONDS))
                .isInstanceOf(com.jeepclub.backend.publications.core.application.exception.EventOperationException.class);
            assertThat(admin.dashboard(e.getId()).registrations()).isEmpty();
        } finally {proceed.countDown();pool.shutdownNow();}
    }
}
