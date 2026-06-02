package com.jeepclub.backend.billing.infra.persistence.adapter;

import com.jeepclub.backend.billing.core.domain.model.ChargeDefinition;
import com.jeepclub.backend.billing.core.repository.ChargeDefinitionRepository;
import com.jeepclub.backend.billing.infra.persistence.entity.ChargeDefinitionEntity;
import com.jeepclub.backend.billing.infra.persistence.jpa.ChargeDefinitionJpaRepository;
import com.jeepclub.backend.billing.infra.persistence.mapper.ChargeDefinitionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChargeDefinitionAdapter implements ChargeDefinitionRepository {

    private final ChargeDefinitionJpaRepository jpa;
    private final ChargeDefinitionMapper mapper;

    @Override
    public ChargeDefinition save(ChargeDefinition chargeDefinition) {
        ChargeDefinitionEntity entity = mapper.toEntity(chargeDefinition);
        ChargeDefinitionEntity savedEntity = jpa.save(entity);

        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<ChargeDefinition> findById(Long id) {
        return jpa.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Page<ChargeDefinition> findAll(Pageable pageable) {
        return jpa.findAll(pageable)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return jpa.existsByName(name);
    }

    @Override
    public boolean existsByNameAndIdNot(String name, Long id) {
        return jpa.existsByNameAndIdNot(name, id);
    }
}