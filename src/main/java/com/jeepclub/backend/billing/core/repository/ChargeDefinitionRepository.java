package com.jeepclub.backend.billing.core.repository;

import com.jeepclub.backend.billing.core.domain.model.ChargeDefinition;

import java.util.List;
import java.util.Optional;

public interface ChargeDefinitionRepository {

    ChargeDefinition save(ChargeDefinition chargeDefinition);

    Optional<ChargeDefinition> findById(Long id);

    List<ChargeDefinition> findAll();

    boolean existsByName(String name);
}