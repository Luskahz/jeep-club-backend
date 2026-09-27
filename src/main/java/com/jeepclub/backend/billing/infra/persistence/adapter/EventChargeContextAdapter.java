package com.jeepclub.backend.billing.infra.persistence.adapter;

import com.jeepclub.backend.billing.core.domain.model.EventChargeContext;
import com.jeepclub.backend.billing.core.repository.EventChargeContextRepository;
import com.jeepclub.backend.billing.infra.persistence.entity.EventChargeContextEntity;
import com.jeepclub.backend.billing.infra.persistence.jpa.EventChargeContextJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository @RequiredArgsConstructor
public class EventChargeContextAdapter implements EventChargeContextRepository {
    private final EventChargeContextJpaRepository jpa;
    public Optional<EventChargeContext> find(Long event, Long definition) {
        return jpa.findByEventIdAndChargeDefinitionId(event, definition).map(this::domain);
    }
    public List<EventChargeContext> findByEvent(Long event) {
        return jpa.findByEventIdOrderByChargeDefinitionId(event).stream().map(this::domain).toList();
    }
    public void save(EventChargeContext value) {
        var entity = new EventChargeContextEntity();
        entity.setEventId(value.eventId()); entity.setChargeDefinitionId(value.chargeDefinitionId());
        entity.setAssignmentId(value.assignmentId()); entity.setCycleId(value.cycleId());
        jpa.saveAndFlush(entity);
    }
    private EventChargeContext domain(EventChargeContextEntity value) {
        return new EventChargeContext(value.getEventId(), value.getChargeDefinitionId(), value.getAssignmentId(), value.getCycleId());
    }
}
