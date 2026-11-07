package com.jeepclub.backend.publications.infra.persistence.query;

import com.jeepclub.backend.publications.core.repository.EventParticipantRepository;
import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository @RequiredArgsConstructor
public class EventParticipantJpaQuery implements EventParticipantRepository {
    private final EntityManager em;
    public Page<Long> confirmedUserIds(Long eventId, Pageable pageable) {
        long total = em.createQuery("select count(r) from EventRegistrationEntity r where r.eventId = :id and r.status = 'CONFIRMED'", Long.class)
                .setParameter("id", eventId).getSingleResult();
        if (pageable.getOffset() >= total) return new PageImpl<>(java.util.List.of(), pageable, total);
        var users = em.createQuery("select r.userId from EventRegistrationEntity r where r.eventId = :id and r.status = 'CONFIRMED' order by r.id", Long.class)
                .setParameter("id", eventId).setFirstResult(Math.toIntExact(pageable.getOffset()))
                .setMaxResults(pageable.getPageSize()).getResultList();
        return new PageImpl<>(users, pageable, total);
    }
    public Map<Long, Long> confirmedCounts(Collection<Long> eventIds) {
        if (eventIds.isEmpty()) return Map.of();
        return em.createQuery("select r.eventId, count(r) from EventRegistrationEntity r where r.eventId in :ids and r.status = 'CONFIRMED' group by r.eventId", Object[].class)
                .setParameter("ids", eventIds).getResultList().stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }
}
