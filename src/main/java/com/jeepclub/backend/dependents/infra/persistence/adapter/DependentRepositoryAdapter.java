package com.jeepclub.backend.dependents.infra.persistence.adapter;

import com.jeepclub.backend.dependents.core.domain.enums.DependentStatus;
import com.jeepclub.backend.dependents.core.domain.exception.DependentAlreadyDeletedException;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;
import com.jeepclub.backend.dependents.core.repository.DependentRepository;
import com.jeepclub.backend.dependents.infra.persistence.entity.DependentEntity;
import com.jeepclub.backend.dependents.infra.persistence.entity.DependentHistoryEntity;
import com.jeepclub.backend.dependents.infra.persistence.jpa.DependentHistoryJpaRepository;
import com.jeepclub.backend.dependents.infra.persistence.jpa.DependentJpaRepository;
import com.jeepclub.backend.dependents.infra.persistence.mapper.DependentHistoryMapper;
import com.jeepclub.backend.dependents.infra.persistence.mapper.DependentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DependentRepositoryAdapter implements DependentRepository {

    private final DependentJpaRepository jpaRepository;
    private final DependentHistoryJpaRepository historyJpaRepository;

    private final DependentMapper mapper;
    private final DependentHistoryMapper historyMapper;

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
    public Optional<Dependent> findActiveById(Long id) {
        return jpaRepository.findByIdAndStatus(
                        id,
                        DependentStatus.ACTIVE
                )
                .map(mapper::toDomain);
    }

    @Override
    public List<Dependent> findAllByUserId(Long userId) {
        return jpaRepository.findAllByUserId(userId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Dependent> findAllActiveByUserId(Long userId) {
        return jpaRepository.findAllByUserIdAndStatus(
                        userId,
                        DependentStatus.ACTIVE
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCpf(String cpf) {
        return jpaRepository.existsByCpf(cpf);
    }

    @Override
    public boolean existsByCpfAndIdNot(
            String cpf,
            Long id
    ) {
        return jpaRepository.existsByCpfAndIdNot(
                cpf,
                id
        );
    }

    @Override
    public boolean existsActiveById(Long id) {
        return jpaRepository.existsByIdAndStatus(
                id,
                DependentStatus.ACTIVE
        );
    }

    @Override
    public boolean existsActiveByIdAndUserId(
            Long id,
            Long userId
    ) {
        return jpaRepository.existsByIdAndUserIdAndStatus(
                id,
                userId,
                DependentStatus.ACTIVE
        );
    }

    @Override
    public void delete(
            Dependent dependent,
            Long deletedByUserId,
            Instant deletedAt
    ) {
        DependentEntity entity = jpaRepository
                .findByIdForUpdate(dependent.getId())
                .orElseThrow(
                        () -> new DependentAlreadyDeletedException(
                                dependent.getId()
                        )
                );

        DependentHistoryEntity history =
                historyMapper.toHistoryEntity(
                        entity,
                        deletedByUserId,
                        deletedAt
                );

        historyJpaRepository.save(history);

        jpaRepository.delete(entity);
    }
}
