package com.jeepclub.backend.storage.core.application;

import com.jeepclub.backend.storage.core.repository.MediaCatalogRepository;
import com.jeepclub.backend.storage.api.module.StorageConsumerPolicy;
import com.jeepclub.backend.storage.core.domain.model.*;
import com.jeepclub.backend.storage.core.domain.enums.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.time.Instant;
import java.util.List;

@Service @Transactional
public class MediaReferenceService {
    private final MediaCatalogRepository catalog;private final List<StorageConsumerPolicy> policies;
    public MediaReferenceService(MediaCatalogRepository catalog,List<StorageConsumerPolicy> policies) {this.catalog=catalog;this.policies=List.copyOf(policies);}
    public long associate(long actor,long mediaId,MediaContext context,String resource,String role) {
        var asset=catalog.findAsset(mediaId).orElseThrow(() -> new AccessDeniedException("Media unavailable"));
        if(!Long.valueOf(actor).equals(asset.ownerUserId()) || !mayChange(actor,context,resource)) throw new AccessDeniedException("Media unavailable");
        Instant now=Instant.now();
        return catalog.recordReference(new MediaReference(null,mediaId,context,resource,role,null,MediaOrigin.UPLOAD,now,actor,null,null,null,null),audit(actor,now)).referenceId();
    }
    public void dissociate(long actor,long mediaId,long referenceId) {
        var reference=catalog.findReference(referenceId).orElseThrow(() -> new AccessDeniedException("Media unavailable"));
        if(!reference.mediaId().equals(mediaId) || !mayChange(actor,reference.context(),reference.consumerId())) throw new AccessDeniedException("Media unavailable");
        if(!reference.isCurrent()) return;
        Instant now=Instant.now();catalog.endReference(reference.end(now,actor,"DETACHED"),audit(actor,now));
    }
    private boolean mayChange(long actor,MediaContext context,String resource) {return policies.stream().filter(p -> p.context()==context).anyMatch(p -> p.mayUpload(actor,resource));}
    private MediaAuditContext audit(long actor,Instant now) {return new MediaAuditContext(MediaActorKind.USER,actor,now,null);}
}
