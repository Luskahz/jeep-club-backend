package com.jeepclub.backend.billing.infra.persistence.jpa;

import com.jeepclub.backend.billing.infra.persistence.entity.ChargeDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargeDefinitionJpaRepository extends JpaRepository<ChargeDefinitionEntity, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select d from ChargeDefinitionEntity d where d.id = :id")
    java.util.Optional<ChargeDefinitionEntity> findByIdForUpdate(Long id);

    org.springframework.data.domain.Page<ChargeDefinitionEntity> findByStatusAndRecurrenceType(
        com.jeepclub.backend.billing.core.domain.enums.definition.ChargeDefinitionStatus status,
        com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType recurrenceType,
        org.springframework.data.domain.Pageable pageable);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
