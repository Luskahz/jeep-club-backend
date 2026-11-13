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

// A reference write must join the owning aggregate's transaction.
@Service @Transactional(propagation=org.springframework.transaction.annotation.Propagation.MANDATORY)
public class MediaReferenceService {
    private final MediaCatalogRepository catalog;private final List<StorageConsumerPolicy> policies;
    public MediaReferenceService(MediaCatalogRepository catalog,List<StorageConsumerPolicy> policies) {this.catalog=catalog;this.policies=List.copyOf(policies);}
    public long associate(long actor,long mediaId,MediaContext context,String resource,String role) {
        return associate(actor,mediaId,context,resource,role,null);
    }
    public long associate(long actor,long mediaId,MediaContext context,String resource,String role,Integer position) {
        validatePosition(position);
        var asset=catalog.findAsset(mediaId).orElseThrow(() -> new AccessDeniedException("Media unavailable"));
        if(actor<=0 || policies.stream().filter(p -> p.context()==context).noneMatch(p -> p.mayAssociate(actor,asset,resource,role))) throw new AccessDeniedException("Media unavailable");
        Instant now=Instant.now();
        return catalog.recordReference(new MediaReference(null,mediaId,context,resource,role,position,MediaOrigin.UPLOAD,now,actor,null,null,null,null),audit(actor,now)).referenceId();
    }
    public void dissociate(long actor,long mediaId,long referenceId) {
        var reference=catalog.findReference(referenceId).orElseThrow(() -> new AccessDeniedException("Media unavailable"));
        if(actor<=0 || !reference.mediaId().equals(mediaId) || policies.stream().filter(p -> p.context()==reference.context()).noneMatch(p -> p.mayDissociate(actor,reference))) throw new AccessDeniedException("Media unavailable");
        if(!reference.isCurrent()) return;
        Instant now=Instant.now();catalog.endReference(reference.end(now,actor,"DETACHED"),audit(actor,now));
    }
    /** Caller locks and changes the consumer aggregate in this same transaction. */
    public long replace(long actor,long oldReferenceId,long newMediaId,MediaContext context,String resource,String role) {
        return replace(actor,oldReferenceId,newMediaId,context,resource,role,null);
    }
    /** Position belongs to the new interval. Reordering ends and recreates intervals, never rewrites history. */
    public long replace(long actor,long oldReferenceId,long newMediaId,MediaContext context,String resource,String role,Integer position) {
        validatePosition(position);
        var previous=catalog.findReference(oldReferenceId).orElseThrow(() -> new AccessDeniedException("Media unavailable"));
        if(previous.context()!=context || !previous.consumerId().equals(resource) || !previous.role().equals(role)
            || policies.stream().filter(p -> p.context()==context).noneMatch(p -> p.mayDissociate(actor,previous)))
            throw new AccessDeniedException("Media unavailable");
        catalog.lockAssets(List.of(previous.mediaId(),newMediaId));
        if(!previous.isCurrent()) throw new jakarta.persistence.OptimisticLockException("Media association changed");
        long next=associate(actor,newMediaId,context,resource,role,position);
        if(next!=oldReferenceId) {
            Instant now=Instant.now();catalog.endReference(previous.end(now,actor,"REPLACED"),audit(actor,now));
        }
        return next;
    }
    private static void validatePosition(Integer position) {
        if(position!=null && position<0) throw new IllegalArgumentException("Invalid media position");
    }
    private MediaAuditContext audit(long actor,Instant now) {return new MediaAuditContext(MediaActorKind.USER,actor,now,null);}
}
