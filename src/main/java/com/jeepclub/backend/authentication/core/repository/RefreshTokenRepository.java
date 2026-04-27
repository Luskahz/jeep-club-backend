package com.jeepclub.backend.authentication.core.repository;

import com.jeepclub.backend.authentication.core.domain.enums.RefreshTokenStatus;
import com.jeepclub.backend.authentication.core.domain.model.RefreshToken;
import com.jeepclub.backend.authentication.core.domain.model.Session;

import java.util.Optional;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken token, Session session);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    Optional<RefreshToken> findBySessionIdAndStatus(Long sessionId, RefreshTokenStatus status);
}