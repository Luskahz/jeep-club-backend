package com.jeepclub.backend.storage.core.application;

import com.jeepclub.backend.storage.core.repository.*;
import com.jeepclub.backend.storage.api.module.StorageConsumerPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.util.List;

@Service
public class MediaResolutionService {
    public record Content(byte[] bytes,String mimeType) {}
    private final MediaCatalogRepository catalog;private final BlobCatalog blobs;private final MediaGovernance governance;
    private final ContextualMediaAccess access;
    public MediaResolutionService(MediaCatalogRepository catalog,BlobCatalog blobs,MediaGovernance governance,List<StorageConsumerPolicy> policies) {
        this.catalog=catalog;this.blobs=blobs;this.governance=governance;
        access=new ContextualMediaAccess((actor,reference,master) -> policies.stream().filter(p -> p.context()==reference.context()).anyMatch(p -> p.mayRead(actor,reference,master)));
    }
    @Transactional(readOnly=true) public Content resolve(long actor,long mediaId,long referenceId,String code) {
        var asset=catalog.findAsset(mediaId).orElseThrow(MediaResolutionService::unavailable);
        var ref=catalog.findReference(referenceId).orElseThrow(MediaResolutionService::unavailable);
        var variant=catalog.findVariant(mediaId,code).orElseThrow(MediaResolutionService::unavailable);
        boolean allowed=governance.allows(mediaId,referenceId);
        var state=allowed ? ContextualMediaAccess.Moderation.ALLOWED : ContextualMediaAccess.Moderation.DENIED;
        if(!access.decide(actor,asset,ref,variant,state,state,true).allowed()) throw unavailable();
        var blob=blobs.findByVariant(variant.variantId()).orElseThrow(MediaResolutionService::unavailable);
        return new Content(blobs.read(blob),variant.mimeType());
    }
    private static AccessDeniedException unavailable() { return new AccessDeniedException("Media unavailable"); }
}
