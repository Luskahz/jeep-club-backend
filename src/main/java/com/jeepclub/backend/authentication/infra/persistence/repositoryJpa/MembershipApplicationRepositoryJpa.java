package com.jeepclub.backend.authentication.infra.persistence.repositoryJpa;

import com.jeepclub.backend.authentication.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.authentication.core.domain.model.MembershipApplication;
import com.jeepclub.backend.authentication.core.repository.MembershipApplicationRepository;
import com.jeepclub.backend.authentication.infra.persistence.entities.MembershipApplicationEntity;
import com.jeepclub.backend.authentication.infra.persistence.jpa.MembershipApplicationJpaRepository;
import com.jeepclub.backend.authentication.infra.persistence.mapper.MembershipApplicationMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MembershipApplicationRepositoryJpa implements MembershipApplicationRepository {

    private final MembershipApplicationJpaRepository jpaRepository;
    private final MembershipApplicationMapper mapper;

    public MembershipApplicationRepositoryJpa(
            MembershipApplicationJpaRepository jpaRepository,
            MembershipApplicationMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public MembershipApplication save(MembershipApplication application) {
        MembershipApplicationEntity entity = mapper.toEntity(application);
        MembershipApplicationEntity savedEntity = jpaRepository.save(entity);

        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<MembershipApplication> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByCpfAndStatus(String cpf, MembershipApplicationStatus status) {
        return jpaRepository.existsByCpfAndStatus(cpf, status);
    }

    @Override
    public boolean existsByEmailAndStatus(String email, MembershipApplicationStatus status) {
        return jpaRepository.existsByEmailAndStatus(email, status);
    }
}