package com.jeepclub.backend.billing.infra.persistence.jpa;

import com.jeepclub.backend.billing.infra.persistence.entity.EventChargeContextEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface EventChargeContextJpaRepository extends JpaRepository<EventChargeContextEntity, Long> {
    Optional<EventChargeContextEntity> findByEventIdAndChargeDefinitionId(Long eventId, Long definitionId);
    List<EventChargeContextEntity> findByEventIdOrderByChargeDefinitionId(Long eventId);
}
