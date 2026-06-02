package com.jeepclub.backend.memberships.infra.persistence.adapter;

import com.jeepclub.backend.memberships.core.domain.model.MemberActivationToken;
import com.jeepclub.backend.memberships.core.repository.MemberActivationTokenRepository;
import com.jeepclub.backend.memberships.infra.persistence.jpa.MemberActivationTokenJpaRepository;
import com.jeepclub.backend.memberships.infra.persistence.mapper.MemberActivationTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberActivationTokenRepositoryAdapter implements MemberActivationTokenRepository {

    private final MemberActivationTokenJpaRepository jpaRepository;

    @Override
    public MemberActivationToken save(MemberActivationToken token) {
        return MemberActivationTokenMapper.toDomain(
                jpaRepository.save(MemberActivationTokenMapper.toEntity(token))
        );
    }

    @Override
    public Optional<MemberActivationToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash)
                .map(MemberActivationTokenMapper::toDomain);
    }

    @Override
    public Optional<MemberActivationToken> findLatestByApplicationId(Long applicationId) {
        return jpaRepository.findLatestByApplicationId(applicationId)
                .map(MemberActivationTokenMapper::toDomain);
    }

    @Override
    @Transactional
    public void invalidateAllByApplicationId(Long applicationId) {
        jpaRepository.invalidateAllByApplicationId(applicationId);
    }
}