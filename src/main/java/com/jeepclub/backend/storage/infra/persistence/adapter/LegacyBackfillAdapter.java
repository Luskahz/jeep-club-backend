package com.jeepclub.backend.storage.infra.persistence.adapter;

import com.jeepclub.backend.storage.core.repository.*;
import com.jeepclub.backend.storage.core.domain.model.*;
import com.jeepclub.backend.storage.core.domain.enums.*;
import com.jeepclub.backend.storage.infra.persistence.entity.LegacyMediaCheckpointEntity;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Repository
public class LegacyBackfillAdapter implements LegacyBackfill {
    private final EntityManager em;private final BlobCatalog blobs;private final LegacyMediaSource source;
    private final MediaCatalogRepository catalog;private final MediaGovernance governance;private final MediaAdministration admin;
    private final boolean enabled;private final TransactionTemplate tx;
    public LegacyBackfillAdapter(EntityManager em,BlobCatalog blobs,LegacyMediaSource source,MediaCatalogRepository catalog,
        MediaGovernance governance,MediaAdministration admin,PlatformTransactionManager manager,@Value("${storage.legacy-backfill.enabled:false}") boolean enabled) {
        this.em=em;this.blobs=blobs;this.source=source;this.catalog=catalog;this.governance=governance;this.admin=admin;this.enabled=enabled;
        tx=new TransactionTemplate(manager);tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    private String id(String key) {return BlobCatalogAdapter.digest(("LOCAL\0"+key).getBytes(StandardCharsets.UTF_8));}
    @Override public Result inspect(String key) {
        byte[] bytes=source.read(key);
        return tx.execute(status -> {var row=em.find(LegacyMediaCheckpointEntity.class,id(key));String state=row==null || row.getMediaId()==null?"UNCATALOGUED":row.isRolledBack()?"ROLLED_BACK":"CATALOGUED";
            if(row!=null && row.getMediaId()!=null && !row.isRolledBack()) {
                var master=catalog.findVariant(row.getMediaId(),"MASTER").orElseThrow();
                if(!BlobCatalogAdapter.digest(bytes).equals(master.checksum())) state="SOURCE_CHANGED";
            }
            return new Result(state,row==null?null:row.getMediaId(),bytes.length);});
    }
    @Override public Result apply(String key,MediaAuditContext actor) {
        if(!enabled) throw new IllegalStateException("Legacy backfill disabled");
        byte[] bytes=source.read(key);String id=id(key);
        try {
            tx.executeWithoutResult(status -> {
                if(em.find(LegacyMediaCheckpointEntity.class,id)!=null) return;
                var row=new LegacyMediaCheckpointEntity();row.setSourceId(id);row.setLegacyKey(key);em.persist(row);em.flush();
            });
        } catch(RuntimeException collision) {
            if(!Boolean.TRUE.equals(tx.execute(status -> em.find(LegacyMediaCheckpointEntity.class,id)!=null))) throw collision;
        }
        var blob=blobs.store(bytes);
        return tx.execute(status -> {
            var row=em.find(LegacyMediaCheckpointEntity.class,id,LockModeType.PESSIMISTIC_WRITE);
            if(row.getMediaId()!=null) {
                var master=catalog.findVariant(row.getMediaId(),"MASTER").orElseThrow();
                if(!blob.sha256().equals(master.checksum())) throw new IllegalStateException("Legacy source changed since checkpoint");
                return new Result(row.isRolledBack()?"ROLLED_BACK":"CATALOGUED",row.getMediaId(),bytes.length);
            }
            Instant now=actor.occurredAt();
            var asset=catalog.registerAsset(MediaAsset.create(MediaOrigin.LEGACY_IMPORT,MediaContext.UNKNOWN,id,null,null,MediaSubjectType.UNKNOWN,null,"LEGACY_CUSTODY",now),actor);
            var variant=catalog.registerVariant(new MediaVariant(null,asset.mediaId(),"MASTER",MediaVariantRole.MASTER,"LOCAL","storage-blobs",blob.storageKey(),"application/octet-stream",blob.sizeBytes(),null,null,"SHA-256",blob.sha256(),null,"LEGACY_V1",null,now),actor);
            blobs.bind(variant.variantId(),blob);governance.initialize(asset.mediaId(),blob.sha256(),true,actor);governance.legalHold(asset.mediaId(),true,actor);
            row.setMediaId(asset.mediaId());em.flush();return new Result("CATALOGUED",asset.mediaId(),bytes.length);
        });
    }
    @Override public void rollback(String key,MediaAuditContext actor) {
        if(!enabled) throw new IllegalStateException("Legacy backfill disabled");
        tx.executeWithoutResult(status -> {
            var row=em.find(LegacyMediaCheckpointEntity.class,id(key),LockModeType.PESSIMISTIC_WRITE);
            if(row==null || row.getMediaId()==null || row.isRolledBack()) return;
            admin.discard(row.getMediaId(),actor);row.setRolledBack(true);em.flush();
        });
    }
}
