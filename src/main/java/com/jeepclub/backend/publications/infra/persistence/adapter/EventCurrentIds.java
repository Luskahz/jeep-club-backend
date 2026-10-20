package com.jeepclub.backend.publications.infra.persistence.adapter;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import java.util.List;

/** Short nonlocking committed ID discovery while the caller holds the Event root.
 * IDs are immutable and children are retained. Never locks an empty child range. */
@Component @RequiredArgsConstructor
public class EventCurrentIds {
    private final EntityManager entityManager;
    @Transactional(propagation=Propagation.REQUIRES_NEW,isolation=Isolation.READ_COMMITTED,readOnly=true)
    public <T> T inCommittedSnapshot(java.util.function.Supplier<T> read) { return read.get(); }
    @Transactional(propagation=Propagation.REQUIRES_NEW,isolation=Isolation.READ_COMMITTED,readOnly=true)
    public <T> List<Long> read(Class<T> entityType,Long eventId) {
        var builder=entityManager.getCriteriaBuilder();var query=builder.createQuery(Long.class);
        var root=query.from(entityType);
        query.select(root.get("id")).where(builder.equal(root.get("eventId"),eventId)).orderBy(builder.asc(root.get("id")));
        return entityManager.createQuery(query).getResultList();
    }
}
