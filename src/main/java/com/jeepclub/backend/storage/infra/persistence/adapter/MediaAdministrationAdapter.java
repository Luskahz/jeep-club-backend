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
        return search(new Search(afterId,limit,context,state,null,null,null,null,null,null,null,null));
    }
    @Override @Transactional(readOnly=true) public List<Entry> search(Search f) {
        if (f==null) throw new IllegalArgumentException("Missing filter");
        page(f.afterId(),f.limit());
        if ((f.uploader()!=null && f.uploader()<=0) || (f.owner()!=null && f.owner()<=0)
                || (f.createdFrom()!=null && f.createdBefore()!=null && !f.createdFrom().isBefore(f.createdBefore()))
                || (f.mime()!=null && !f.mime().matches("[a-z0-9.+-]{1,60}/[a-z0-9.+-]{1,60}"))
                || (f.state()!=null && !java.util.Set.of("DISCARDED","VISIBLE","UNDER_REVIEW","HIDDEN","BLOCKED").contains(f.state()))
                || (f.moderation()!=null && !java.util.Set.of("VISIBLE","UNDER_REVIEW","HIDDEN","BLOCKED").contains(f.moderation())))
            throw new IllegalArgumentException("Invalid media filter");
        var jpql=new StringBuilder("select a,c from MediaAssetEntity a,MediaControlEntity c where a.mediaId=c.mediaId and a.mediaId>:after");
        if(f.context()!=null) jpql.append(" and a.sourceContext=:context");
        if(f.state()==null) jpql.append(" and c.discarded=false");
        else if(f.state().equals("DISCARDED")) jpql.append(" and c.discarded=true");
        else jpql.append(" and c.discarded=false and c.state=:state");
        if(f.uploader()!=null) jpql.append(" and a.uploadedByUserId=:uploader");
        if(f.owner()!=null) jpql.append(" and a.ownerUserId=:owner");
        if(f.createdFrom()!=null) jpql.append(" and a.createdAt>=:fromDate");
        if(f.createdBefore()!=null) jpql.append(" and a.createdAt<:beforeDate");
        if(f.mime()!=null) jpql.append(" and exists (select v.variantId from MediaVariantEntity v where v.mediaId=a.mediaId and v.mimeType=:mime)");
        if(f.moderation()!=null) jpql.append(" and c.state=:moderation");
        if(f.physicalState()!=null) jpql.append(" and a.physicalState=:physical");
        if(f.usage()==Usage.ACTIVE) jpql.append(" and exists (select r.referenceId from MediaReferenceEntity r where r.mediaId=a.mediaId and r.unlinkedAt is null)");
        if(f.usage()==Usage.HISTORICAL_ONLY) jpql.append(" and exists (select r.referenceId from MediaReferenceEntity r where r.mediaId=a.mediaId and r.unlinkedAt is not null) and not exists (select r.referenceId from MediaReferenceEntity r where r.mediaId=a.mediaId and r.unlinkedAt is null)");
        if(f.usage()==Usage.UNREFERENCED) jpql.append(" and not exists (select r.referenceId from MediaReferenceEntity r where r.mediaId=a.mediaId)");
        var q=em.createQuery(jpql.append(" order by a.mediaId").toString(),Object[].class).setParameter("after",f.afterId()).setMaxResults(f.limit());
        if(f.context()!=null) q.setParameter("context",f.context());
        if(f.state()!=null && !f.state().equals("DISCARDED")) q.setParameter("state",f.state());
        if(f.uploader()!=null) q.setParameter("uploader",f.uploader());
        if(f.owner()!=null) q.setParameter("owner",f.owner());
        if(f.createdFrom()!=null) q.setParameter("fromDate",f.createdFrom());
        if(f.createdBefore()!=null) q.setParameter("beforeDate",f.createdBefore());
        if(f.mime()!=null) q.setParameter("mime",f.mime());
        if(f.moderation()!=null) q.setParameter("moderation",f.moderation());
        if(f.physicalState()!=null) q.setParameter("physical",f.physicalState());
        return q.getResultList().stream().map(row -> {
                var a=(MediaAssetEntity)row[0];var c=(MediaControlEntity)row[1];return new Entry(a.getMediaId(),a.getSourceContext(),a.getCreatedAt(),c.isDiscarded()?"DISCARDED":c.getState(),c.isSensitive(),c.isLegalHold());
            }).toList();
    }
    @Override @Transactional(readOnly=true) public Detail detail(long mediaId,long afterVariantId,long afterReferenceId,int limit) {
        if (mediaId<=0) throw new IllegalArgumentException("Invalid media id");
        page(afterVariantId,limit);page(afterReferenceId,limit);
        var rows=em.createQuery("select a,c from MediaAssetEntity a,MediaControlEntity c where a.mediaId=c.mediaId and a.mediaId=:id",Object[].class)
                .setParameter("id",mediaId).getResultList();
        if (rows.isEmpty()) throw new com.jeepclub.backend.shared.storage.exception.StorageObjectNotFoundException();
        var a=(MediaAssetEntity)rows.get(0)[0];var c=(MediaControlEntity)rows.get(0)[1];
        var variants=em.createQuery("select v from MediaVariantEntity v where v.mediaId=:id and v.variantId>:after order by v.variantId",MediaVariantEntity.class)
                .setParameter("id",mediaId).setParameter("after",afterVariantId).setMaxResults(limit+1).getResultList();
        var references=em.createQuery("select r from MediaReferenceEntity r where r.mediaId=:id and r.referenceId>:after order by r.referenceId",MediaReferenceEntity.class)
                .setParameter("id",mediaId).setParameter("after",afterReferenceId).setMaxResults(limit+1).getResultList();
        Long nextVariant=variants.size()>limit ? variants.get(limit-1).getVariantId() : null;
        Long nextReference=references.size()>limit ? references.get(limit-1).getReferenceId() : null;
        return new Detail(mediaId,a.getSourceContext(),a.getOrigin().name(),a.getClassification().name(),a.getPhysicalState().name(),
                c.getState(),c.isDiscarded(),c.isSensitive(),c.isLegalHold(),a.getCreatedAt(),a.getUpdatedAt(),
                variants.stream().limit(limit).map(v->new Variant(v.getVariantId(),v.getVariantCode(),v.getRole().name(),v.getMimeType(),v.getSizeBytes(),v.getWidth(),v.getHeight())).toList(),nextVariant,
                references.stream().limit(limit).map(r->new Reference(r.getReferenceId(),r.getContext(),r.getRole(),r.getPosition(),r.getLinkedAt(),r.getUnlinkedAt())).toList(),nextReference);
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
        var e=new MediaModerationEntity();e.setMediaId(id);e.setOperation(operation);e.setState(state);if(operation.equals("LEGACY_REAPPLIED")){e.setReason("BACKFILL_CUSTODY");e.setInternalNote("Revalidated source; unknown legacy provenance remains held");}e.setActorUserId(actor.actorUserId());e.setOccurredAt(actor.occurredAt());em.persist(e);em.flush();
    }
    private void page(long after,int limit) { if(after<0 || limit<1 || limit>100) throw new IllegalArgumentException("Invalid page"); }
}
