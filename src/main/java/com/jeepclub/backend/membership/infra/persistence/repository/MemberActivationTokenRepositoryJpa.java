package com.jeepclub.backend.membership.infra.persistence.repository;

import com.jeepclub.backend.membership.core.domain.model.MemberActivationToken;
import com.jeepclub.backend.membership.core.repository.MemberActivationTokenRepository;
import com.jeepclub.backend.membership.infra.persistence.jpa.MemberActivationTokenJpaRepository;
import com.jeepclub.backend.membership.infra.persistence.mapper.MemberActivationTokenMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public class MemberActivationTokenRepositoryJpa implements MemberActivationTokenRepository {

    private final MemberActivationTokenJpaRepository jpaRepository;
    private final MemberActivationTokenMapper mapper;

    public MemberActivationTokenRepositoryJpa(
            MemberActivationTokenJpaRepository jpaRepository,
            MemberActivationTokenMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public MemberActivationToken save(MemberActivationToken token) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(token)));
    }

    @Override
    public Optional<MemberActivationToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public Optional<MemberActivationToken> findLatestByApplicationId(Long applicationId) {
        return jpaRepository.findTopByApplicationIdOrderByCreatedAtDesc(applicationId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void invalidateAllByApplicationId(Long applicationId) {
        jpaRepository.invalidateAllByApplicationId(applicationId, Instant.now());
    }
}