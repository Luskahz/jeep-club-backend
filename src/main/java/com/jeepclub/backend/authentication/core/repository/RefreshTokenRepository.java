package com.jeepclub.backend.authentication.core.repository;

import com.jeepclub.backend.authentication.core.domain.model.RefreshToken;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken token);

    List<RefreshToken> findAll();

    Optional<RefreshToken> findById(Long id);

    Optional<RefreshToken> findByIdForUpdate(Long id);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    Optional<Long> findSessionIdByTokenHash(String tokenHash);

    Optional<RefreshToken> findByTokenHashForUpdate(
            String tokenHash
    );

    List<RefreshToken> findByUserId(Long userId);

    void revokeActiveByUserId(Long userId);

    void revokeActiveBySessionId(Long sessionId);
}
