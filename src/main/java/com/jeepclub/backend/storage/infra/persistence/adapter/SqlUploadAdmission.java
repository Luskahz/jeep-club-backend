package com.jeepclub.backend.storage.infra.persistence.adapter;

import com.jeepclub.backend.storage.core.repository.UploadAdmission;
import com.jeepclub.backend.storage.infra.persistence.entity.*;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

/** Storage custody budgets, not a generic request limiter. Unknown/crashed reservations remain charged for review. */
@Repository
public class SqlUploadAdmission implements UploadAdmission {
    private static final long MAX_RESERVATION=20L*1024*1024;
    private final EntityManager em;private final TransactionTemplate tx;
    private final long userBytes,globalBytes,maxUploads;private final int hourly,concurrent;
    public SqlUploadAdmission(EntityManager em,PlatformTransactionManager manager,
        @Value("${storage.upload.max-user-bytes:536870912}") long userBytes,
        @Value("${storage.upload.max-global-bytes:10737418240}") long globalBytes,
        @Value("${storage.upload.max-hourly-attempts:30}") int hourly,
        @Value("${storage.upload.max-concurrent-per-user:2}") int concurrent,
        @Value("${storage.upload.max-retained-uploads-per-user:1000}") long maxUploads) {
        if(userBytes<MAX_RESERVATION || globalBytes<MAX_RESERVATION || hourly<1 || concurrent<1 || concurrent>10 || maxUploads<1) throw new IllegalArgumentException("Invalid upload budget");
        this.em=em;this.userBytes=userBytes;this.globalBytes=globalBytes;this.hourly=hourly;this.concurrent=concurrent;this.maxUploads=maxUploads;
        tx=new TransactionTemplate(manager);tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    private void ensure(long scope) {
        try {tx.executeWithoutResult(status -> {
            if(em.find(UploadBudgetEntity.class,scope)!=null) return;
            var row=new UploadBudgetEntity();row.setScopeId(scope);row.setWindowStart(Instant.now());
            Long existing=scope==0?em.createQuery("select coalesce(sum(b.sizeBytes),0) from StoredBlobEntity b",Long.class).getSingleResult():
                em.createQuery("select coalesce(sum(v.sizeBytes),0) from MediaVariantEntity v,MediaAssetEntity a where v.mediaId=a.mediaId and a.uploadedByUserId=:actor",Long.class).setParameter("actor",scope).getSingleResult();
            row.setUsedBytes(existing);if(scope!=0) row.setChargedUploads(em.createQuery("select count(a) from MediaAssetEntity a where a.uploadedByUserId=:actor",Long.class).setParameter("actor",scope).getSingleResult());
            em.persist(row);em.flush();
        });} catch(RuntimeException collision) {if(!Boolean.TRUE.equals(tx.execute(s -> em.find(UploadBudgetEntity.class,scope)!=null))) throw collision;}
    }
    private UploadBudgetEntity lock(long scope) {return em.find(UploadBudgetEntity.class,scope,LockModeType.PESSIMISTIC_WRITE);}
    @Override public Permit reserve(long actor) {
        if(actor<=0) throw new IllegalArgumentException("Invalid actor");ensure(0);ensure(actor);
        return tx.execute(status -> {
            var global=lock(0);var user=lock(actor);Instant now=Instant.now();
            if(!now.isBefore(user.getWindowStart().plusSeconds(3600))) {user.setWindowStart(now);user.setWindowAttempts(0);}
            if(user.getWindowAttempts()>=hourly || user.getInFlight()>=concurrent || user.getChargedUploads()>=maxUploads
                || user.getUsedBytes()>userBytes-MAX_RESERVATION || global.getUsedBytes()>globalBytes-MAX_RESERVATION) throw new LimitExceeded();
            user.setWindowAttempts(user.getWindowAttempts()+1);user.setInFlight(user.getInFlight()+1);
            user.setUsedBytes(user.getUsedBytes()+MAX_RESERVATION);global.setUsedBytes(global.getUsedBytes()+MAX_RESERVATION);
            var permit=new UploadPermitEntity();permit.setId(UUID.randomUUID().toString());permit.setActorUserId(actor);permit.setAccountedBytes(MAX_RESERVATION);permit.setCreatedAt(now);em.persist(permit);em.flush();
            return new Permit(permit.getId(),actor);
        });
    }
    @Override public void charge(Permit permit,long bytes) {
        if(bytes<=0 || bytes>MAX_RESERVATION) throw new IllegalArgumentException("Invalid representation budget");
        tx.executeWithoutResult(status -> {
            var global=lock(0);var user=lock(permit.actor());var row=em.find(UploadPermitEntity.class,permit.id(),LockModeType.PESSIMISTIC_WRITE);
            if(row==null || row.getActorUserId()!=permit.actor() || row.isCompleted() || row.isCharged()) throw new IllegalArgumentException("Invalid upload permit");
            long difference=bytes-row.getAccountedBytes();global.setUsedBytes(global.getUsedBytes()+difference);user.setUsedBytes(user.getUsedBytes()+difference);
            user.setChargedUploads(user.getChargedUploads()+1);row.setAccountedBytes(bytes);row.setCharged(true);em.flush();
        });
    }
    @Override public void finish(Permit permit) {
        tx.executeWithoutResult(status -> {
            var global=lock(0);var user=lock(permit.actor());var row=em.find(UploadPermitEntity.class,permit.id(),LockModeType.PESSIMISTIC_WRITE);
            if(row==null || row.getActorUserId()!=permit.actor()) throw new IllegalArgumentException("Invalid upload permit");
            if(row.isCompleted()) return;
            if(!row.isCharged()){global.setUsedBytes(global.getUsedBytes()-row.getAccountedBytes());user.setUsedBytes(user.getUsedBytes()-row.getAccountedBytes());row.setAccountedBytes(0);}
            user.setInFlight(user.getInFlight()-1);row.setCompleted(true);em.flush();
        });
    }
}
