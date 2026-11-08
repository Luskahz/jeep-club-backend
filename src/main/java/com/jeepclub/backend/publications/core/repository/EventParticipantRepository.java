package com.jeepclub.backend.publications.core.repository;

import java.util.Collection;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EventParticipantRepository {
    Page<Long> confirmedUserIds(Long eventId, Pageable pageable);
    Map<Long, Long> confirmedCounts(Collection<Long> eventIds);
}
