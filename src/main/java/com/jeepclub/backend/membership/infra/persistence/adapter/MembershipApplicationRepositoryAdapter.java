package com.jeepclub.backend.membership.infra.persistence.adapter;

import com.jeepclub.backend.membership.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.membership.core.domain.model.MembershipApplication;
import com.jeepclub.backend.membership.core.repository.MembershipApplicationRepository;
import com.jeepclub.backend.membership.infra.persistence.entity.MembershipApplicationEntity;
import com.jeepclub.backend.membership.infra.persistence.jpa.MembershipApplicationJpaRepository;
import com.jeepclub.backend.membership.infra.persistence.mapper.MembershipApplicationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MembershipApplicationRepositoryAdapter implements MembershipApplicationRepository {

    private final MembershipApplicationJpaRepository jpaRepository;

    @Override
    public MembershipApplication save(MembershipApplication application) {
        MembershipApplicationEntity entity = MembershipApplicationMapper.toEntity(application);
        MembershipApplicationEntity saved = jpaRepository.save(entity);
        return MembershipApplicationMapper.toDomain(saved);
    }

    @Override
    public Optional<MembershipApplication> findById(Long id) {
        return jpaRepository.findById(id)
                .map(MembershipApplicationMapper::toDomain);
    }

    @Override
    public Optional<MembershipApplication> findByCpf(String cpf) {
        return jpaRepository.findByCpf(cpf)
                .map(MembershipApplicationMapper::toDomain);
    }

    @Override
    public List<MembershipApplication> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(MembershipApplicationMapper::toDomain)
                .toList();
    }

    @Override
    public List<MembershipApplication> findAllByStatus(MembershipApplicationStatus status) {
        return jpaRepository.findAllByStatus(status)
                .stream()
                .map(MembershipApplicationMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCpf(String cpf) {
        return jpaRepository.existsByCpf(cpf);
    }
}