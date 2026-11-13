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
    private final LegacyMediaSource legacy;
    public MediaResolutionService(MediaCatalogRepository catalog,BlobCatalog blobs,MediaGovernance governance,List<StorageConsumerPolicy> policies,LegacyMediaSource legacy) {
        this.legacy=legacy;
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
        // Provider routing is explicit and happens only after the contextual decision.
        // A missing blob binding must never cause a fallback to a client-supplied key.
        if(!"LOCAL".equals(variant.provider())) throw unavailable();
        byte[] bytes;
        if("storage-blobs".equals(variant.namespace())) {
            var blob=blobs.findByVariant(variant.variantId()).orElseThrow(MediaResolutionService::unavailable);
            if(!"SHA-256".equals(variant.checksumAlgorithm()) || !blob.sha256().equals(variant.checksum())
                || blob.sizeBytes()!=variant.sizeBytes() || !blob.storageKey().equals(variant.storageKey())) throw new MediaIntegrityException();
            bytes=blobs.read(blob);
        } else if(java.util.Set.of("images","private-images").contains(variant.namespace())) {
            if(!variant.storageKey().startsWith(variant.namespace()+"/") || !"SHA-256".equals(variant.checksumAlgorithm())) throw unavailable();
            bytes=legacy.read(variant.storageKey());
        } else throw unavailable();
        if(bytes.length!=variant.sizeBytes() || !digest(bytes).equals(variant.checksum())) throw new MediaIntegrityException();
        return new Content(bytes,variant.mimeType());
    }
    private static String digest(byte[] bytes) {
        try {return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(java.security.NoSuchAlgorithmException impossible) {throw new IllegalStateException(impossible);}
    }
    private static AccessDeniedException unavailable() { return new AccessDeniedException("Media unavailable"); }
}
