package com.jeepclub.backend.billing.infra.persistence.jpa;

import com.jeepclub.backend.billing.infra.persistence.entity.EventChargeContextEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface EventChargeContextJpaRepository extends JpaRepository<EventChargeContextEntity, Long> {
    Optional<EventChargeContextEntity> findByEventIdAndChargeDefinitionId(Long eventId, Long definitionId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<EventChargeContextEntity> findWithLockingByEventIdAndChargeDefinitionId(Long eventId, Long chargeDefinitionId);
    List<EventChargeContextEntity> findByEventIdOrderByChargeDefinitionId(Long eventId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<EventChargeContextEntity> findFirstByChargeDefinitionIdOrderById(Long definitionId);
}
