package com.jeepclub.backend.authentication.infra.persistence.adapter;

import com.jeepclub.backend.authentication.core.domain.model.PasswordChangeChallenge;
import com.jeepclub.backend.authentication.core.repository.PasswordChangeChallengeRepository;
import com.jeepclub.backend.authentication.infra.persistence.entity.PasswordChangeChallengeEntity;
import com.jeepclub.backend.authentication.infra.persistence.jpa.PasswordChangeChallengeJpaRepository;
import com.jeepclub.backend.authentication.infra.persistence.mapper.PasswordChangeChallengeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PasswordChangeChallengeRepositoryAdapter
        implements PasswordChangeChallengeRepository {

    private final PasswordChangeChallengeJpaRepository jpaRepository;

    @Override
    public PasswordChangeChallenge save(
            PasswordChangeChallenge challenge
    ) {
        PasswordChangeChallengeEntity entity =
                PasswordChangeChallengeMapper.toEntity(
                        challenge
                );

        PasswordChangeChallengeEntity savedEntity =
                jpaRepository.save(entity);

        return PasswordChangeChallengeMapper.toDomain(
                savedEntity
        );
    }

    @Override
    public Optional<PasswordChangeChallenge> findByTokenHash(
            String tokenHash
    ) {
        return jpaRepository.findByTokenHash(tokenHash)
                .map(PasswordChangeChallengeMapper::toDomain);
    }

    @Override
    public Optional<Long> findUserIdByTokenHash(
            String tokenHash
    ) {
        return jpaRepository.findUserIdByTokenHash(
                tokenHash
        );
    }

    @Override
    public Optional<PasswordChangeChallenge>
    findByTokenHashForUpdate(
            String tokenHash
    ) {
        return jpaRepository
                .findByTokenHashForUpdate(tokenHash)
                .map(PasswordChangeChallengeMapper::toDomain);
    }

    @Override
    public void invalidateActiveByUserId(
            Long userId,
            Instant now
    ) {
        jpaRepository.invalidateActiveByUserId(
                userId,
                now
        );
    }
}
