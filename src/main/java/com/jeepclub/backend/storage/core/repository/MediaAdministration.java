package com.jeepclub.backend.storage.core.repository;

import com.jeepclub.backend.storage.core.domain.enums.MediaContext;
import com.jeepclub.backend.storage.core.domain.enums.MediaPhysicalState;
import com.jeepclub.backend.storage.core.domain.model.MediaAuditContext;
import java.time.Instant;
import java.util.List;

public interface MediaAdministration {
    record Entry(long mediaId,MediaContext context,Instant createdAt,String state,boolean sensitive,boolean legalHold) {}
    record Event(long id,Long referenceId,String operation,String state,String reason,Instant occurredAt) {}
    record Variant(long id,String code,String role,String mime,long bytes,Integer width,Integer height) {}
    record Reference(long id,MediaContext context,String role,Integer position,Instant linkedAt,Instant unlinkedAt) {}
    record Detail(long mediaId,MediaContext context,String origin,String classification,String physicalState,
                  String moderation,boolean discarded,boolean sensitive,boolean legalHold,Instant createdAt,Instant updatedAt,
                  List<Variant> variants,Long nextVariantId,List<Reference> references,Long nextReferenceId) {}
    enum Usage { ACTIVE, HISTORICAL_ONLY, UNREFERENCED }
    record Search(long afterId,int limit,MediaContext context,String state,Long uploader,Long owner,Instant createdFrom,
                  Instant createdBefore,String mime,Usage usage,String moderation,MediaPhysicalState physicalState) {}
    record RetentionReview(String decision,String retentionClass,Instant unusedSince,Instant reviewAfter) {}
    List<Entry> list(long afterId,int limit,MediaContext context,String state);
    List<Entry> search(Search filter);
    Detail detail(long mediaId,long afterVariantId,long afterReferenceId,int limit);
    List<Event> events(long mediaId,long afterId,int limit);
    RetentionReview review(long mediaId,MediaAuditContext actor);
    /** Internal backfill only: content/checkpoint validation precedes this transition. */
    void reapplyLegacy(long mediaId,MediaAuditContext actor);
    void discard(long mediaId,MediaAuditContext actor);
}
