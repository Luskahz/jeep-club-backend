package com.jeepclub.backend.dependents.api.controller;

import org.springframework.test.context.*;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
class DependentPhotoMySqlIT extends DependentPhotoIntegrationTest {
    @org.springframework.beans.factory.annotation.Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean com.jeepclub.backend.dependents.core.application.service.dependent.DependentPhotoAuthorization authorization;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean com.jeepclub.backend.platform.logging.SystemLogJpaRepository auditRows;
    @org.junit.jupiter.api.Test void auditCommitFailureCannotDeliverBytesOrPersistSuccess() throws Exception {
        Long id=id(create(ownerToken,cpf(),PNG).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated()));
        org.mockito.Mockito.doAnswer(a -> {
            var saved=a.callRealMethod();
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                @Override public void beforeCommit(boolean readOnly) {throw new IllegalStateException("synthetic audit commit failure");}
            });
            return saved;
        }).when(auditRows).saveAndFlush(org.mockito.ArgumentMatchers.argThat(e -> e.getAction().equals("DEPENDENT_PHOTO_READ")));
        read(id,ownerToken).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isInternalServerError())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentTypeCompatibleWith("application/problem+json"));
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("select count(*) from platform_logs where action='DEPENDENT_PHOTO_READ' and path=?",Long.class,"/dependents/"+id)).isZero();
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"PERMISSION","SESSION","DEPENDENT","IDENTITY"})
    void revocationAfterHttpFilterButBeforeSnapshotIsRespected(String type) throws Exception {
        Long id=id(create(ownerToken,cpf(),PNG).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated()));String original=key(id);
        var before=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);var pool=java.util.concurrent.Executors.newSingleThreadExecutor();
        org.mockito.Mockito.doAnswer(a -> {before.countDown();await(release);return a.callRealMethod();}).when(authorization)
            .authorize(org.mockito.ArgumentMatchers.eq(id),org.mockito.ArgumentMatchers.eq(owner),org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyBoolean());
        boolean administrative=type.equals("PERMISSION");
        try {
            var reading=pool.submit(() -> administrative
                ?mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/users/{owner}/dependents/{id}/photo",owner,id).header("Authorization",readerToken)).andReturn().getResponse()
                :read(id,ownerToken).andReturn().getResponse());
            org.assertj.core.api.Assertions.assertThat(before.await(20,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            switch(type) {
                case "PERMISSION" -> jdbc.update("delete from authorization_user_roles where user_id=?",reader);
                case "SESSION" -> tx(() -> {sessions.revokeActiveByUserId(owner);return null;});
                case "DEPENDENT" -> dependents.archive(id,owner,owner);
                case "IDENTITY" -> identities.disable(owner,java.time.Instant.now(clock));
            }
            release.countDown();var response=reading.get(10,java.util.concurrent.TimeUnit.SECONDS);
            org.assertj.core.api.Assertions.assertThat(response.getStatus()).isIn(403,404);
            org.mockito.Mockito.verify(storage,org.mockito.Mockito.never()).load(original);
        } finally {release.countDown();pool.shutdownNow();}
    }
    @org.junit.jupiter.api.Test void slowStorageContentionEvidence() throws Exception {
        Long id=id(create(ownerToken,cpf(),PNG).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated()));
        String original=key(id);var entered=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
        var calls=new java.util.concurrent.atomic.AtomicInteger();var pool=java.util.concurrent.Executors.newFixedThreadPool(3);
        org.mockito.Mockito.doAnswer(a -> {
            if(calls.incrementAndGet()==1) {entered.countDown();if(!release.await(20,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("Fixture timeout");}
            return a.callRealMethod();
        }).when(storage).load(original);
        try {
            var first=pool.submit(() -> dependents.photo(id,owner));
            org.assertj.core.api.Assertions.assertThat(entered.await(20,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            long started=System.nanoTime();
            var second=pool.submit(() -> dependents.photo(id,owner));
            boolean baseline=Boolean.getBoolean("photo.baseline");
            if(!baseline)org.assertj.core.api.Assertions.assertThat(second.get(5,java.util.concurrent.TimeUnit.SECONDS).content()).containsExactly(PNG);
            var patch=pool.submit(() -> dependents.removePhoto(id,owner));
            long waitCount=0;
            if(baseline) {
                long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
                while(waitCount<2 && System.nanoTime()<deadline)
                    waitCount=jdbc.queryForObject("select count(*) from performance_schema.data_lock_waits",Long.class);
                org.assertj.core.api.Assertions.assertThat(waitCount).isGreaterThanOrEqualTo(2);
                // Fixed, artificial storage delay, after the database has proved contention.
                new java.util.concurrent.CountDownLatch(1).await(1200,java.util.concurrent.TimeUnit.MILLISECONDS);
                release.countDown();
            } else {
                patch.get(5,java.util.concurrent.TimeUnit.SECONDS);
                org.assertj.core.api.Assertions.assertThat(first.isDone()).isFalse();
                waitCount=jdbc.queryForObject("select count(*) from performance_schema.data_lock_waits",Long.class);
                org.assertj.core.api.Assertions.assertThat(waitCount).isZero();
            }
            long elapsed=java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started);
            System.out.println("PHOTO_CONTENTION mode="+(baseline?"before":"after")+" concurrent_get_and_patch_ms="+elapsed+" observed_lock_waits="+waitCount);
            release.countDown();
            org.assertj.core.api.Assertions.assertThat(first.get(10,java.util.concurrent.TimeUnit.SECONDS).content()).containsExactly(PNG);
            if(baseline) {second.get(10,java.util.concurrent.TimeUnit.SECONDS);patch.get(10,java.util.concurrent.TimeUnit.SECONDS);}
        } finally {release.countDown();pool.shutdownNow();}
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"REPLACE","REMOVE","DISABLE","ARCHIVE","DELETE","OWNER_DISABLE","ADMIN_PERMISSION"})
    void authorizedSnapshotCanFinishWhileConcurrentRevocationCommits(String change) throws Exception {
        Long id=id(create(ownerToken,cpf(),PNG).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated()));
        String original=key(id);var entered=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        org.mockito.Mockito.doAnswer(a -> {entered.countDown();await(release);return a.callRealMethod();}).when(storage).load(original);
        boolean admin=change.equals("ADMIN_PERMISSION");
        try {
            var reading=pool.submit(() -> admin?dependents.photoForAdmin(id,owner,reader):dependents.photo(id,owner));
            org.assertj.core.api.Assertions.assertThat(entered.await(20,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            var mutation=pool.submit(() -> {
                switch(change) {
                    case "REPLACE" -> dependents.replacePhoto(id,owner,new com.jeepclub.backend.platform.storage.image.ImageUpload("photo.png","image/png",png(2)));
                    case "REMOVE" -> dependents.removePhoto(id,owner);
                    case "DISABLE" -> dependents.disable(id,owner);
                    case "ARCHIVE" -> dependents.archive(id,owner,owner);
                    case "DELETE" -> dependents.delete(id,owner);
                    case "OWNER_DISABLE" -> identities.disable(owner,java.time.Instant.now(clock));
                    case "ADMIN_PERMISSION" -> jdbc.update("delete from authorization_user_roles where user_id=?",reader);
                }
                return true;
            });
            org.assertj.core.api.Assertions.assertThat(mutation.get(5,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            org.assertj.core.api.Assertions.assertThat(reading.isDone()).isFalse();
            if(change.equals("REPLACE"))org.assertj.core.api.Assertions.assertThat(dependents.photo(id,owner).content()).containsExactly(png(2));
            else org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
                if(admin)dependents.photoForAdmin(id,owner,reader);else dependents.photo(id,owner);
            }).isInstanceOf(RuntimeException.class);
            release.countDown();org.assertj.core.api.Assertions.assertThat(reading.get(10,java.util.concurrent.TimeUnit.SECONDS).content()).containsExactly(PNG);
            org.mockito.Mockito.verify(storage,org.mockito.Mockito.never()).delete(original);
        } finally {release.countDown();pool.shutdownNow();}
    }
    @org.junit.jupiter.api.Test void oldCallerSnapshotCannotReadAfterCommittedArchival() throws Exception {
        Long id=id(create(ownerToken,cpf(),PNG).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated()));
        var seen=new java.util.concurrent.CountDownLatch(1);var changed=new java.util.concurrent.CountDownLatch(1);var pool=java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            var reading=pool.submit(() -> {
                try {return tx(() -> {jpa.findById(id).orElseThrow();seen.countDown();await(changed);return dependents.photo(id,owner);});}
                catch(RuntimeException ex) {return ex;}
            });
            org.assertj.core.api.Assertions.assertThat(seen.await(20,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            dependents.archive(id,owner,owner);changed.countDown();
            org.assertj.core.api.Assertions.assertThat(reading.get(10,java.util.concurrent.TimeUnit.SECONDS))
                .isInstanceOf(com.jeepclub.backend.dependents.core.application.exception.DependentNotFoundException.class);
        } finally {changed.countDown();pool.shutdownNow();}
    }
    @Container static final MySQLContainer MYSQL=new MySQLContainer("mysql:8.0.46").withUsername("root").withPassword("synthetic-private-photo-test-only");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",MYSQL::getJdbcUrl);registry.add("spring.datasource.username",MYSQL::getUsername);
        registry.add("spring.datasource.password",MYSQL::getPassword);registry.add("spring.datasource.driver-class-name",() -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.database-platform",() -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto",() -> "validate");registry.add("spring.flyway.enabled",() -> true);
    }
}
