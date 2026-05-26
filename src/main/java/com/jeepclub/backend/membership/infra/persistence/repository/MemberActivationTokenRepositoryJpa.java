package com.jeepclub.backend.membership.infra.persistence.repository;

import com.jeepclub.backend.membership.core.domain.model.MemberActivationToken;
import com.jeepclub.backend.membership.core.repository.MemberActivationTokenRepository;
import com.jeepclub.backend.membership.infra.persistence.jpa.MemberActivationTokenJpaRepository;
import com.jeepclub.backend.membership.infra.persistence.mapper.MemberActivationTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor

//escolher entre  persistence.adapter e repositoryJpa ambos são a mesma coisa, eu recomendo que vc padronize tudo em persistence.adapter
// o adapter do membershipApplication que vai virar MembershipRequest e o adapter do MemberActivationTokenMapper.
public class MemberActivationTokenRepositoryJpa implements MemberActivationTokenRepository {

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