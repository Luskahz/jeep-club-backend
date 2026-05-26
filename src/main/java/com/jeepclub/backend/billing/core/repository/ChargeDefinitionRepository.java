package com.jeepclub.backend.billing.core.repository;

import com.jeepclub.backend.billing.core.domain.model.ChargeDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ChargeDefinitionRepository {

    ChargeDefinition save(ChargeDefinition chargeDefinition);

    Optional<ChargeDefinition> findById(Long id);

    Page<ChargeDefinition> findAll(Pageable pageable);

    boolean existsByName(String name);
}