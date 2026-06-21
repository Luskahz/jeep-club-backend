package com.jeepclub.backend.authentication.core.repository;

import com.jeepclub.backend.authentication.core.domain.enums.PasswordRecoveryRequestMethod;
import com.jeepclub.backend.authentication.core.domain.model.PasswordRecoveryRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PasswordRecoveryRequestRepository {

    PasswordRecoveryRequest save(PasswordRecoveryRequest request);

    List<PasswordRecoveryRequest> findAll();

    Optional<PasswordRecoveryRequest> findById(Long id);

    List<PasswordRecoveryRequest> findByUserId(Long userId);

    Optional<PasswordRecoveryRequest> findByTokenHash(String tokenHash);
    Optional<PasswordRecoveryRequest> findByTokenHashForUpdate(
            String tokenHash
    );

    Optional<PasswordRecoveryRequest> findOpenByUserId(
            Long userId,
            Instant now
    );

    Optional<PasswordRecoveryRequest> findOpenByUserIdAndMethod(
            Long userId,
            PasswordRecoveryRequestMethod method,
            Instant now
    );


}