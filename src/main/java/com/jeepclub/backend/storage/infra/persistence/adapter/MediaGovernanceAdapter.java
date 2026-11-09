package com.jeepclub.backend.storage.infra.persistence.adapter;

import com.jeepclub.backend.storage.core.repository.MediaGovernance;
import com.jeepclub.backend.storage.core.domain.model.*;
import com.jeepclub.backend.storage.infra.persistence.entity.*;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository @Transactional
public class MediaGovernanceAdapter implements MediaGovernance {
    private final EntityManager em;
    public MediaGovernanceAdapter(EntityManager em) { this.em=em; }
    @Override public void initialize(long id,String digest,boolean sensitive,MediaAuditContext audit) {
        var row=new MediaControlEntity(); row.setMediaId(id);row.setReceivedSha256(digest);row.setState("VISIBLE");row.setSensitive(sensitive);
        em.persist(row); event(id,null,"UPLOAD_COMPLETED","VISIBLE",null,null,audit.actorUserId(),audit.occurredAt());
    }
    @Override @Transactional(readOnly=true) public boolean allows(long mediaId,long referenceId) {
        var row=em.find(MediaControlEntity.class,mediaId);
        if(row==null || row.isDiscarded() || !"VISIBLE".equals(row.getState())) return false;
        var reference=em.find(MediaReferenceEntity.class,referenceId);
        if(reference==null || !reference.getMediaId().equals(mediaId)) return false;
        var events=em.createQuery("select e from MediaModerationEntity e, MediaReferenceEntity r where e.referenceId=r.referenceId and e.mediaId=:media and r.context=:context and r.consumerId=:consumer and r.role=:role and e.operation='MODERATION' order by e.id desc",MediaModerationEntity.class)
            .setParameter("media",mediaId).setParameter("context",reference.getContext())
            .setParameter("consumer",reference.getConsumerId()).setParameter("role",reference.getRole()).setMaxResults(1).getResultList();
        return events.isEmpty() || "VISIBLE".equals(events.get(0).getState());
    }
    @Override public void moderate(ModerationDecision decision) {
        var row=em.find(MediaControlEntity.class,decision.mediaId(),LockModeType.PESSIMISTIC_WRITE);
        if(row==null || row.isDiscarded()) throw new IllegalArgumentException("Unknown or discarded media");
        if(decision.referenceId()==null) row.setState(decision.state().name());
        else {
            var ref=em.find(MediaReferenceEntity.class,decision.referenceId());
            if(ref==null || !ref.getMediaId().equals(decision.mediaId())) throw new IllegalArgumentException("Reference mismatch");
        }
        event(decision.mediaId(),decision.referenceId(),"MODERATION",decision.state().name(),decision.reason().name(),decision.internalNote(),decision.actorUserId(),decision.occurredAt());
    }
    @Override public void previewAccess(long mediaId,long referenceId,MediaAuditContext audit) {
        var reference=em.find(MediaReferenceEntity.class,referenceId);
        if(reference==null || !reference.getMediaId().equals(mediaId)) throw new IllegalArgumentException("Reference mismatch");
        event(mediaId,referenceId,"ADMIN_PREVIEW",null,null,null,audit.actorUserId(),audit.occurredAt());
    }
    @Override public void legalHold(long id,boolean held,String justification,MediaAuditContext audit) {
        if(justification==null || justification.isBlank() || justification.length()>2000) throw new IllegalArgumentException("Hold justification required");
        var row=em.find(MediaControlEntity.class,id,LockModeType.PESSIMISTIC_WRITE);
        if(row==null) throw new IllegalArgumentException("Unknown media"); row.setLegalHold(held);
        event(id,null,held ? "LEGAL_HOLD_SET" : "LEGAL_HOLD_RELEASED",null,"RETENTION_OBLIGATION",justification.trim(),audit.actorUserId(),audit.occurredAt());
    }
    private void event(long id,Long reference,String operation,String state,String reason,String note,Long actor,java.time.Instant at) {
        var e=new MediaModerationEntity();e.setMediaId(id);e.setReferenceId(reference);e.setOperation(operation);e.setState(state);e.setReason(reason);e.setInternalNote(note);e.setActorUserId(actor);e.setOccurredAt(at);em.persist(e);em.flush();
    }
}
