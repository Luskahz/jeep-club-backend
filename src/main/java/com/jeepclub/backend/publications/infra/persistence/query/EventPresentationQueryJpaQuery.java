package com.jeepclub.backend.publications.infra.persistence.query;
import com.jeepclub.backend.publications.api.module.EventPresentationQuery;
import com.jeepclub.backend.publications.core.repository.EventPresentationQueryRepository;
@org.springframework.stereotype.Repository @lombok.RequiredArgsConstructor
public class EventPresentationQueryJpaQuery implements EventPresentationQueryRepository {
    private final jakarta.persistence.EntityManager em;
    public java.util.List<EventPresentationQuery.Details> findByIds(java.util.Collection<Long> ids) {
        if (ids.isEmpty()) return java.util.List.of();
        var result = new java.util.ArrayList<>(em.createQuery("select e.id,e.title,e.startsAt from EventEntity e where e.id in :ids",Object[].class).setParameter("ids",ids)
            .getResultList().stream().map(t->new EventPresentationQuery.Details((Long)t[0],(String)t[1],(java.time.Instant)t[2])).toList());
        var missing = new java.util.HashSet<>(ids);
        result.forEach(e -> missing.remove(e.id()));
        if (!missing.isEmpty()) result.addAll(em.createQuery("select e.publicationId,e.title,e.startsAt from EventHistoryEntity e where e.publicationId in :ids",Object[].class).setParameter("ids",missing)
            .getResultList().stream().map(t->new EventPresentationQuery.Details((Long)t[0],(String)t[1],(java.time.Instant)t[2])).toList());
        return java.util.List.copyOf(result);
    }
    public java.util.Optional<HistoricalEvent> findHistoricalById(Long id) {
        return em.createQuery("select e.publicationId,e.title,e.startsAt,e.endsAt,e.eventStatus,e.deletedAt from EventHistoryEntity e where e.publicationId=:id",Object[].class)
            .setParameter("id",id).getResultList().stream().map(t -> new HistoricalEvent((Long)t[0],(String)t[1],
                (java.time.Instant)t[2],(java.time.Instant)t[3],
                (com.jeepclub.backend.publications.core.domain.enums.EventStatus)t[4],(java.time.Instant)t[5])).findFirst();
    }
}
