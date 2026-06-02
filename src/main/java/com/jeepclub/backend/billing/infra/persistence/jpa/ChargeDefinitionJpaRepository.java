package com.jeepclub.backend.billing.infra.persistence.jpa;

import com.jeepclub.backend.billing.infra.persistence.entity.ChargeDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargeDefinitionJpaRepository extends JpaRepository<ChargeDefinitionEntity, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}