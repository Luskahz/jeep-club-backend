package com.jeepclub.backend.storage.core.application;

import com.jeepclub.backend.storage.api.module.StorageConsumerPolicy;
import com.jeepclub.backend.storage.core.repository.*;
import com.jeepclub.backend.storage.core.domain.model.*;
import com.jeepclub.backend.storage.core.domain.enums.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.access.AccessDeniedException;
import java.time.Instant;
import java.util.List;

@Service
public class MediaUploadService {
    /** An upload has custody but no operational reference until a consumer attaches it. */
    public record Uploaded(long mediaId,Long referenceId) {}
    private final UploadAdmission admission;private final MediaCatalogRepository catalog; private final BlobCatalog blobs;
    private final MediaImageProcessor images; private final MediaGovernance governance;
    private final List<StorageConsumerPolicy> policies; private final TransactionTemplate transaction;private final io.micrometer.core.instrument.MeterRegistry metrics;
    public MediaUploadService(MediaCatalogRepository catalog,BlobCatalog blobs,MediaImageProcessor images,MediaGovernance governance,
        List<StorageConsumerPolicy> policies,PlatformTransactionManager manager,io.micrometer.core.instrument.MeterRegistry metrics,UploadAdmission admission) {
        this.admission=admission;this.metrics=metrics;this.catalog=catalog;this.blobs=blobs;this.images=images;this.governance=governance;this.policies=List.copyOf(policies);transaction=new TransactionTemplate(manager);
    }
    public Uploaded upload(long actor,MediaContext context,String consumerId,byte[] bytes) {
        return upload(actor,context,consumerId,() -> bytes);
    }
    public Uploaded upload(long actor,MediaContext context,String consumerId,java.util.function.Supplier<byte[]> content) {
        return upload(actor,context,consumerId,content,MediaImageProcessor.Options.defaults());
    }
    public Uploaded upload(long actor,MediaContext context,String consumerId,java.util.function.Supplier<byte[]> content,MediaImageProcessor.Options options) {
        long started=System.nanoTime();try {
        if(actor<=0) throw new AccessDeniedException("Media unavailable");
        var policy=policies.stream().filter(p -> p.context()==context && p.mayUpload(actor,consumerId)).findFirst()
            .orElseThrow(() -> new AccessDeniedException("Media unavailable"));
        var identity=policy.uploadIdentity(actor,consumerId);
        var permit=admission.reserve(actor);
        try {
        var processed=images.process(content.get(),context,options);
        admission.charge(permit,processed.images().stream().mapToLong(i -> i.bytes().length).sum());
        var stored=processed.images().stream().map(i -> blobs.store(i.bytes())).toList();
        return transaction.execute(status -> {
            Instant now=Instant.now(); var audit=new MediaAuditContext(MediaActorKind.USER,actor,now,null);
            var asset=catalog.registerAsset(new MediaAsset(null,MediaOrigin.UPLOAD,context,consumerId,actor,identity.ownerUserId(),identity.subjectType(),identity.subjectId(),
                "PHOTO",MediaClassification.CLASSIFIED,MediaPhysicalState.PRESENT,now,now,null),audit);
            for(int i=0;i<processed.images().size();i++) {
                var image=processed.images().get(i);var blob=stored.get(i);
                var variant=catalog.registerVariant(new MediaVariant(null,asset.mediaId(),image.code(),image.code().equals("MASTER") ? MediaVariantRole.MASTER : MediaVariantRole.DERIVED,
                    "LOCAL","storage-blobs",blob.storageKey(),image.mimeType(),blob.sizeBytes(),image.width(),image.height(),"SHA-256",blob.sha256(),image.code(),"V1",null,now),audit);
                blobs.bind(variant.variantId(),blob);
            }
            governance.initialize(asset.mediaId(),processed.receivedSha256(),java.util.Set.of(MediaContext.DEPENDENT_PHOTO,MediaContext.MEDICAL_IMAGE,MediaContext.IDENTIFICATION_PHOTO).contains(context),audit);
            return new Uploaded(asset.mediaId(),null);
        });
        } finally {admission.finish(permit);}
        } finally { metrics.timer("storage.upload.duration").record(System.nanoTime()-started,java.util.concurrent.TimeUnit.NANOSECONDS); }
    }
}
