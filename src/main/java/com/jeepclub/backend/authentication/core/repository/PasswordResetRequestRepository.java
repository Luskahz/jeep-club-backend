package com.jeepclub.backend.authentication.core.repository;

import com.jeepclub.backend.authentication.core.domain.model.PasswordResetRequest;

import java.util.Optional;

public interface PasswordResetRequestRepository {
    PasswordResetRequest save(PasswordResetRequest request);
    Optional<PasswordResetRequest> findByTokenHash(String tokenHash);
}
