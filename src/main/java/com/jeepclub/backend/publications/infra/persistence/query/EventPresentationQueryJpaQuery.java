package com.jeepclub.backend.publications.infra.persistence.query;
import com.jeepclub.backend.publications.api.module.EventPresentationQuery;
import com.jeepclub.backend.publications.core.repository.EventPresentationQueryRepository;
@org.springframework.stereotype.Repository @lombok.RequiredArgsConstructor
public class EventPresentationQueryJpaQuery implements EventPresentationQueryRepository {
    private final jakarta.persistence.EntityManager em;
    public java.util.List<EventPresentationQuery.Details> findByIds(java.util.Collection<Long> ids) {
        return em.createQuery("select e.id,e.title,e.startsAt from EventEntity e where e.id in :ids",Object[].class).setParameter("ids",ids)
            .getResultList().stream().map(t->new EventPresentationQuery.Details((Long)t[0],(String)t[1],(java.time.Instant)t[2])).toList();
    }
}
