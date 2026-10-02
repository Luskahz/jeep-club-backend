package com.jeepclub.backend.publications.core.repository;
import com.jeepclub.backend.publications.api.module.EventPresentationQuery;
public interface EventPresentationQueryRepository {
    java.util.List<EventPresentationQuery.Details> findByIds(java.util.Collection<Long> ids);
    record HistoricalEvent(Long id, String title, java.time.Instant startsAt, java.time.Instant endsAt,
        com.jeepclub.backend.publications.core.domain.enums.EventStatus status, java.time.Instant deletedAt) {}
    java.util.Optional<HistoricalEvent> findHistoricalById(Long id);
}
