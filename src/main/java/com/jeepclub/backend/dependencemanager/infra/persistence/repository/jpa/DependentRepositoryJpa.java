package com.jeepclub.backend.dependencemanager.infra.persistence.repository.jpa;

import com.jeepclub.backend.dependencemanager.core.domain.model.Dependent;
import com.jeepclub.backend.dependencemanager.core.repository.DependentRepository;
import com.jeepclub.backend.dependencemanager.infra.persistence.entity.DependentEntity;
import com.jeepclub.backend.dependencemanager.infra.persistence.jpa.DependentJpaRepository;
import com.jeepclub.backend.dependencemanager.infra.persistence.mapper.DependentMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class DependentRepositoryJpa implements DependentRepository {

    private final DependentJpaRepository jpaRepository;
    private final DependentMapper mapper;

    public DependentRepositoryJpa(
            DependentJpaRepository jpaRepository,
            DependentMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Dependent save(Dependent dependent) {
        DependentEntity entity = mapper.toEntity(dependent);
        DependentEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Dependent> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<Dependent> findAllBySocioId(Long socioId) {
        return jpaRepository.findAllBySocioId(socioId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsByCpf(String cpf) {
        return jpaRepository.existsByCpf(cpf);
    }

    @Override
    public boolean existsByCpfAndIdNot(String cpf, Long id) {
        return jpaRepository.existsByCpfAndIdNot(cpf, id);
    }
}

