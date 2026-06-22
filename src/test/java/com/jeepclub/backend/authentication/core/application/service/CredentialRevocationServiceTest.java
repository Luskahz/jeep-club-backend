package com.jeepclub.backend.authentication.core.application.service;

import com.jeepclub.backend.authentication.core.repository.PasswordChangeChallengeRepository;
import com.jeepclub.backend.authentication.core.repository.RefreshTokenRepository;
import com.jeepclub.backend.authentication.core.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class CredentialRevocationServiceTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordChangeChallengeRepository challengeRepository;
    @InjectMocks
    private CredentialRevocationService service;

    @Test
    void revokesEveryCredentialForUser() {
        Instant now = Instant.parse("2026-06-22T12:00:00Z");

        service.revokeAllForUser(7L, now);

        var ordered = inOrder(refreshTokenRepository, sessionRepository, challengeRepository);
        ordered.verify(refreshTokenRepository).revokeActiveByUserId(7L);
        ordered.verify(sessionRepository).revokeActiveByUserId(7L);
        ordered.verify(challengeRepository).invalidateActiveByUserId(7L, now);
    }
}
