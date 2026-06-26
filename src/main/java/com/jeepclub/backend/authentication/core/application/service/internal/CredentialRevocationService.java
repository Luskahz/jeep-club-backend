package com.jeepclub.backend.authentication.core.application.service.internal;

import com.jeepclub.backend.authentication.core.repository.PasswordChangeChallengeRepository;
import com.jeepclub.backend.authentication.core.repository.RefreshTokenRepository;
import com.jeepclub.backend.authentication.core.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CredentialRevocationService {

    private final SessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordChangeChallengeRepository passwordChangeChallengeRepository;

    public void revokeAllForUser(Long userId, Instant now) {
        refreshTokenRepository.revokeActiveByUserId(userId);
        sessionRepository.revokeActiveByUserId(userId);
        passwordChangeChallengeRepository.invalidateActiveByUserId(userId, now);
    }
}
