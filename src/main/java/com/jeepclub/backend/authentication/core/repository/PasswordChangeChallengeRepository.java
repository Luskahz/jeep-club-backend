package com.jeepclub.backend.authentication.core.repository;

import com.jeepclub.backend.authentication.core.domain.model.PasswordChangeChallenge;

import java.time.Instant;
import java.util.Optional;

public interface PasswordChangeChallengeRepository {

    PasswordChangeChallenge save(
            PasswordChangeChallenge challenge
    );

    Optional<PasswordChangeChallenge> findByTokenHash(
            String tokenHash
    );

    Optional<Long> findUserIdByTokenHash(
            String tokenHash
    );

    Optional<PasswordChangeChallenge> findByTokenHashForUpdate(
            String tokenHash
    );

    int invalidateActiveByUserId(
            Long userId,
            Instant now
    );
}
