package com.jeepclub.backend.billing.core.repository;

import com.jeepclub.backend.billing.core.domain.model.EventChargeContext;
import java.util.List;
import java.util.Optional;

public interface EventChargeContextRepository {
    Optional<EventChargeContext> find(Long eventId, Long definitionId);
    List<EventChargeContext> findByEvent(Long eventId);
    void save(EventChargeContext context);
}
