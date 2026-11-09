package com.jeepclub.backend.storage.infra.persistence.adapter;

import com.jeepclub.backend.storage.core.repository.MediaAdministration;
import com.jeepclub.backend.storage.core.domain.enums.MediaContext;
import com.jeepclub.backend.storage.core.domain.model.MediaAuditContext;
import com.jeepclub.backend.storage.infra.persistence.entity.*;
import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.List;

@Repository @Transactional
public class MediaAdministrationAdapter implements MediaAdministration {
    private final EntityManager em; private final long commonDays,sensitiveDays;
    public MediaAdministrationAdapter(EntityManager em,
        @Value("${STORAGE_UNUSED_COMMON_RETENTION_DAYS:365}") long commonDays,
        @Value("${STORAGE_UNUSED_SENSITIVE_RETENTION_DAYS:90}") long sensitiveDays) {
        if(commonDays<=0 || sensitiveDays<=0 || commonDays>36500 || sensitiveDays>36500) throw new IllegalArgumentException("Invalid retention policy");
        this.em=em;this.commonDays=commonDays;this.sensitiveDays=sensitiveDays;
    }
    @Override @Transactional(readOnly=true) public List<Entry> list(long afterId,int limit,MediaContext context,String state) {
        page(afterId,limit);
        return em.createQuery("select a,c from MediaAssetEntity a,MediaControlEntity c where a.mediaId=c.mediaId and a.mediaId>:after and (:context is null or a.sourceContext=:context) and ((:state is null and c.discarded=false) or (:state='DISCARDED' and c.discarded=true) or (c.discarded=false and c.state=:state)) order by a.mediaId",Object[].class)
            .setParameter("after",afterId).setParameter("context",context).setParameter("state",state).setMaxResults(limit).getResultList().stream().map(row -> {
                var a=(MediaAssetEntity)row[0];var c=(MediaControlEntity)row[1];return new Entry(a.getMediaId(),a.getSourceContext(),a.getCreatedAt(),c.isDiscarded()?"DISCARDED":c.getState(),c.isSensitive(),c.isLegalHold());
            }).toList();
    }
    @Override @Transactional(readOnly=true) public List<Event> events(long mediaId,long afterId,int limit) {
        page(afterId,limit);
        return em.createQuery("select e from MediaModerationEntity e where e.mediaId=:media and e.id>:after order by e.id",MediaModerationEntity.class)
            .setParameter("media",mediaId).setParameter("after",afterId).setMaxResults(limit).getResultList().stream()
            .map(e -> new Event(e.getId(),e.getReferenceId(),e.getOperation(),e.getState(),e.getReason(),e.getOccurredAt())).toList();
    }
    @Override public RetentionReview review(long mediaId,MediaAuditContext actor) {
        var asset=em.find(MediaAssetEntity.class,mediaId,LockModeType.PESSIMISTIC_WRITE);
        var control=em.find(MediaControlEntity.class,mediaId,LockModeType.PESSIMISTIC_WRITE);
        if(asset==null || control==null) throw new IllegalArgumentException("Unknown media");
        var rows=em.createQuery("select r from MediaReferenceEntity r where r.mediaId=:media order by r.referenceId",MediaReferenceEntity.class)
            .setParameter("media",mediaId).setMaxResults(10001).getResultList();
        var policy=new com.jeepclub.backend.storage.core.domain.policy.RetentionPolicy(commonDays,sensitiveDays);
        var assessment=policy.assess(com.jeepclub.backend.storage.infra.persistence.mapper.MediaCatalogMapper.toDomain(asset),
            rows.stream().map(com.jeepclub.backend.storage.infra.persistence.mapper.MediaCatalogMapper::toDomain).toList(),
            rows.size()<=10000,control.isLegalHold(),actor.occurredAt());
        String decision=assessment.decision().name();
        if(!control.isLegalHold() && asset.getClassification()==com.jeepclub.backend.storage.core.domain.enums.MediaClassification.UNCLASSIFIED)
            decision="INDETERMINATE";
        if(assessment.retentionClass()==com.jeepclub.backend.storage.core.domain.policy.RetentionPolicy.RetentionClass.SENSITIVE) control.setSensitive(true);
        event(mediaId,"RETENTION_REVIEW",decision,actor);
        return new RetentionReview(decision,control.isSensitive()?"SENSITIVE":"COMMON",assessment.unusedSince(),assessment.reviewAfter());
    }
    @Override public void discard(long mediaId,MediaAuditContext actor) {
        var asset=em.find(MediaAssetEntity.class,mediaId,LockModeType.PESSIMISTIC_WRITE);
        var control=em.find(MediaControlEntity.class,mediaId,LockModeType.PESSIMISTIC_WRITE);
        if(asset==null || control==null || active(mediaId)>0) throw new IllegalArgumentException("Media still in use or unknown");
        control.setDiscarded(true);event(mediaId,"LOGICAL_DISCARD","DISCARDED",actor);
    }
    @Override public void reapplyLegacy(long mediaId,MediaAuditContext actor) {
        var asset=em.find(MediaAssetEntity.class,mediaId,LockModeType.PESSIMISTIC_WRITE);
        var control=em.find(MediaControlEntity.class,mediaId,LockModeType.PESSIMISTIC_WRITE);
        if(asset==null || control==null || asset.getOrigin()!=com.jeepclub.backend.storage.core.domain.enums.MediaOrigin.LEGACY_IMPORT
            || asset.getOwnerUserId()!=null || asset.getUploadedByUserId()!=null || asset.getSourceContext()!=MediaContext.UNKNOWN
            || em.createQuery("select count(r) from MediaReferenceEntity r where r.mediaId=:media",Long.class).setParameter("media",mediaId).getSingleResult()!=0)
            throw new IllegalArgumentException("Legacy custody cannot be reapplied");
        if(!control.isDiscarded()) throw new IllegalArgumentException("Legacy custody is not rolled back");
        control.setDiscarded(false);control.setLegalHold(true);
        event(mediaId,"LEGACY_REAPPLIED",control.getState(),actor);
    }
    private long active(long mediaId) { return em.createQuery("select count(r) from MediaReferenceEntity r where r.mediaId=:media and r.unlinkedAt is null",Long.class).setParameter("media",mediaId).getSingleResult(); }
    private void event(long id,String operation,String state,MediaAuditContext actor) {
        var e=new MediaModerationEntity();e.setMediaId(id);e.setOperation(operation);e.setState(state);e.setActorUserId(actor.actorUserId());e.setOccurredAt(actor.occurredAt());em.persist(e);em.flush();
    }
    private void page(long after,int limit) { if(after<0 || limit<1 || limit>100) throw new IllegalArgumentException("Invalid page"); }
}
