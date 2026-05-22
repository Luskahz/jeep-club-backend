package com.jeepclub.backend.authentication.infra.persistence.jpa;

import com.jeepclub.backend.authentication.infra.persistence.entity.PasswordChangeChallengeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordChangeChallengeJpaRepository
        extends JpaRepository<PasswordChangeChallengeEntity, Long> {

    Optional<PasswordChangeChallengeEntity> findByTokenHash(String tokenHash);

    List<PasswordChangeChallengeEntity> findByUserIdAndUsedFalse(Long userId);
}