package com.jeepclub.backend.storage.core.repository;

import com.jeepclub.backend.storage.core.domain.enums.MediaContext;
import com.jeepclub.backend.storage.core.domain.model.MediaAuditContext;
import java.time.Instant;
import java.util.List;

public interface MediaAdministration {
    record Entry(long mediaId,MediaContext context,Instant createdAt,String state,boolean sensitive,boolean legalHold) {}
    record Event(long id,Long referenceId,String operation,String state,String reason,Instant occurredAt) {}
    record RetentionReview(String decision,String retentionClass,Instant unusedSince,Instant reviewAfter) {}
    List<Entry> list(long afterId,int limit,MediaContext context,String state);
    List<Event> events(long mediaId,long afterId,int limit);
    RetentionReview review(long mediaId,MediaAuditContext actor);
    void discard(long mediaId,MediaAuditContext actor);
}
