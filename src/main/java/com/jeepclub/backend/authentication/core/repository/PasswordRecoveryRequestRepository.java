package com.jeepclub.backend.authentication.core.repository;

import com.jeepclub.backend.authentication.core.domain.enums.PasswordRecoveryRequestMethod;
import com.jeepclub.backend.authentication.core.domain.model.PasswordRecoveryRequest;

import java.time.Instant;
import java.util.Optional;

public interface PasswordRecoveryRequestRepository {
    Optional<PasswordRecoveryRequest> findByTokenHash(String tokenHash);

    Optional<PasswordRecoveryRequest> findOpenByUserId(
            Long userId,
            Instant now
    );

    Optional<PasswordRecoveryRequest> findOpenByUserIdAndMethod(
            Long userId,
            PasswordRecoveryRequestMethod method,
            Instant now
    );

    PasswordRecoveryRequest save(PasswordRecoveryRequest request);
}
