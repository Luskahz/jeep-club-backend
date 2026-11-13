package com.jeepclub.backend.storage.infra.persistence;

import org.testcontainers.junit.jupiter.*;
import org.testcontainers.mysql.MySQLContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@Testcontainers
class StorageBlobMySqlIT extends StorageUploadFlowTest {
    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named="storage.dedup.timing",matches="true")
    void diagnosticTimingNewAndPreviouslyStoredSyntheticBlobs() throws Exception {
        var rng=new java.util.Random(537);
        var existing=new java.util.ArrayList<byte[]>();var fresh=new java.util.ArrayList<byte[]>();
        for(int i=0;i<50;i++) {byte[] a=new byte[65536],b=new byte[65536];rng.nextBytes(a);rng.nextBytes(b);existing.add(a);fresh.add(b);blobs.store(a);}
        for(int i=0;i<10;i++) {byte[] warm=new byte[65536];rng.nextBytes(warm);blobs.store(warm);blobs.store(warm);}
        record Request(boolean reused,byte[] data) {}
        var requests=new java.util.ArrayList<Request>();
        for(int i=0;i<50;i++) {requests.add(new Request(true,existing.get(i)));requests.add(new Request(false,fresh.get(i)));}
        java.util.Collections.shuffle(requests,new java.util.Random(1234));
        var reuseTimes=new java.util.ArrayList<Long>();var freshTimes=new java.util.ArrayList<Long>();
        for(var request:requests) {long start=System.nanoTime();blobs.store(request.data());long us=java.util.concurrent.TimeUnit.NANOSECONDS.toMicros(System.nanoTime()-start);(request.reused()?reuseTimes:freshTimes).add(us);}
        java.util.Collections.sort(reuseTimes);java.util.Collections.sort(freshTimes);
        System.out.println("BLOB_TIMING_DIAGNOSTIC n=50 bytes=65536 warmup=10 order=randomized single-thread mysql=8.0.46 new_median_us="+freshTimes.get(24)+" new_p95_us="+freshTimes.get(47)+" reused_median_us="+reuseTimes.get(24)+" reused_p95_us="+reuseTimes.get(47));
    }
    @org.junit.jupiter.api.Test void identicalStoreWaitsForExistingBlobLockAndReusesTheSameBytes() throws Exception {
        byte[] bytes=image(221);
        var original=blobs.store(bytes);
        var executor=java.util.concurrent.Executors.newSingleThreadExecutor();
        try (var blocker=java.sql.DriverManager.getConnection(MYSQL.getJdbcUrl(),MYSQL.getUsername(),MYSQL.getPassword());
             var observer=java.sql.DriverManager.getConnection(MYSQL.getJdbcUrl(),"root",MYSQL.getPassword())) {
            blocker.setAutoCommit(false);
            long blockerId;
            try (var statement=blocker.createStatement();var row=statement.executeQuery("select connection_id()")) {
                row.next();blockerId=row.getLong(1);
            }
            try (var lock=blocker.prepareStatement("select sha256 from storage_blobs where sha256=? for update")) {
                lock.setString(1,original.sha256());
                try (var row=lock.executeQuery()) {org.assertj.core.api.Assertions.assertThat(row.next()).isTrue();}
            }
            var pending=executor.submit(() -> blobs.store(bytes));
            boolean observed=false;
            try (var query=observer.prepareStatement("select count(*) from performance_schema.data_lock_waits w join performance_schema.data_locks l on l.ENGINE_LOCK_ID=w.REQUESTING_ENGINE_LOCK_ID join performance_schema.data_locks b on b.ENGINE_LOCK_ID=w.BLOCKING_ENGINE_LOCK_ID join performance_schema.threads t on t.THREAD_ID=b.THREAD_ID where l.OBJECT_SCHEMA=database() and l.OBJECT_NAME='storage_blobs' and t.PROCESSLIST_ID=?")) {
                query.setLong(1,blockerId);
                long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(15);
                while(System.nanoTime()<deadline && !observed) {
                    try(var rows=query.executeQuery()) {rows.next();observed=rows.getLong(1)>0;}
                    if(!observed) Thread.sleep(50);
                }
                org.assertj.core.api.Assertions.assertThat(observed).as("MySQL reports a wait on storage_blobs").isTrue();
                org.assertj.core.api.Assertions.assertThat(pending.isDone()).isFalse();
            } finally {blocker.rollback();}
            org.assertj.core.api.Assertions.assertThat(pending.get(20,java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(original);
            org.assertj.core.api.Assertions.assertThat(blobs.read(original)).containsExactly(bytes);
            org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("select count(*) from storage_blobs where sha256=?",Long.class,original.sha256())).isEqualTo(1);
        } finally {executor.shutdownNow();}
    }
    @org.junit.jupiter.api.Test void uppercaseConstraintIsEnforcedAfterFlywayEvolution() throws Exception {
        var a=upload.upload(88,com.jeepclub.backend.storage.core.domain.enums.MediaContext.IDENTITY_PROFILE,"88",image(88));
        var v=catalog.findVariant(a.mediaId(),"AVATAR_SMALL").orElseThrow();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update("update storage_media_variants set variant_code='avatar_small' where id=?",v.variantId()))
            .isInstanceOf(RuntimeException.class);
    }
    @Container static final MySQLContainer MYSQL=new MySQLContainer("mysql:8.0.46");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",MYSQL::getJdbcUrl);r.add("spring.datasource.username",MYSQL::getUsername);
        r.add("spring.datasource.password",MYSQL::getPassword);r.add("spring.datasource.driver-class-name",() -> "com.mysql.cj.jdbc.Driver");
        r.add("spring.jpa.database-platform",() -> "org.hibernate.dialect.MySQLDialect");
        r.add("spring.jpa.hibernate.ddl-auto",() -> "validate");r.add("spring.flyway.enabled",() -> true);
    }
}
