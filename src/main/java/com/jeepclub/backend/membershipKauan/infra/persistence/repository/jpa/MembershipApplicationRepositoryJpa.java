package com.jeepclub.backend.membershipKauan.infra.persistence.repository.jpa;

import com.jeepclub.backend.membershipKauan.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.membershipKauan.core.domain.model.MembershipApplication;
import com.jeepclub.backend.membershipKauan.core.repository.MembershipApplicationRepository;
import com.jeepclub.backend.membershipKauan.infra.persistence.entity.MembershipApplicationEntity;
import com.jeepclub.backend.membershipKauan.infra.persistence.jpa.MembershipApplicationJpaRepository;
import com.jeepclub.backend.membershipKauan.infra.persistence.mapper.MembershipApplicationMapper;
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