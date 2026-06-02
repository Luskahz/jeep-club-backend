package com.jeepclub.backend.memberships.infra.persistence.adapter;

import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;
import com.jeepclub.backend.memberships.core.repository.MembershipApplicationRepository;
import com.jeepclub.backend.memberships.infra.persistence.entity.MembershipApplicationEntity;
import com.jeepclub.backend.memberships.infra.persistence.jpa.MembershipApplicationJpaRepository;
import com.jeepclub.backend.memberships.infra.persistence.mapper.MembershipApplicationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public Page<MembershipApplication> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable)
                .map(MembershipApplicationMapper::toDomain);
    }

    @Override
    public Page<MembershipApplication> findAllByStatus(MembershipApplicationStatus status, Pageable pageable) {
        return jpaRepository.findAllByStatus(status, pageable)
                .map(MembershipApplicationMapper::toDomain);
    }

    @Override
    public boolean existsByCpf(String cpf) {
        return jpaRepository.existsByCpf(cpf);
    }
}