package com.jeepclub.backend.authentication.core.application.service.session;

import com.jeepclub.backend.authentication.core.application.result.MeResult;
import com.jeepclub.backend.authentication.core.application.service.internal.CredentialRevocationService;
import com.jeepclub.backend.authentication.core.application.service.internal.PasswordChangeChallengeIssuer;
import com.jeepclub.backend.authentication.core.application.service.internal.TokenIssuanceService;
import com.jeepclub.backend.authentication.core.domain.enums.SessionStatus;
import com.jeepclub.backend.authentication.core.domain.model.Session;
import com.jeepclub.backend.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.authentication.core.port.RefreshTokenHashService;
import com.jeepclub.backend.authentication.core.repository.PasswordChangeChallengeRepository;
import com.jeepclub.backend.authentication.core.repository.PasswordRecoveryRequestRepository;
import com.jeepclub.backend.authentication.core.repository.RefreshTokenRepository;
import com.jeepclub.backend.authentication.core.repository.SessionRepository;
import com.jeepclub.backend.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.identity.api.module.UserQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceMeTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock private AuthenticationAccountRepository accountRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordChangeChallengeRepository challengeRepository;
    @Mock private PasswordRecoveryRequestRepository recoveryRequestRepository;
    @Mock private PasswordHasher passwordHasher;
    @Mock private RefreshTokenHashService tokenHashService;
    @Mock private CredentialRevocationService credentialRevocationService;
    @Mock private PasswordChangeChallengeIssuer challengeIssuer;
    @Mock private TokenIssuanceService tokenIssuanceService;
    @Mock private UserQuery identityQuery;
    @Mock private Clock clock;

    @InjectMocks
    private SessionService service;

    @Test
    void currentSessionReturnsOnlyAuthenticationOwnedData() {
        Session session = Session.reconstitute(
                7L,
                42L,
                NOW.minusSeconds(300),
                NOW.plusSeconds(3600),
                null,
                SessionStatus.ACTIVE
        );
        when(clock.instant()).thenReturn(NOW);
        when(sessionRepository.findById(7L)).thenReturn(Optional.of(session));

        MeResult result = service.getCurrentSession(
                42L,
                7L,
                NOW.plusSeconds(900)
        );

        assertThat(result.userId()).isEqualTo(42L);
        assertThat(result.sessionId()).isEqualTo(7L);
        assertThat(result.sessionActive()).isTrue();
        assertThat(result.expiresInSeconds()).isEqualTo(900);
        verifyNoInteractions(accountRepository, identityQuery);
    }
}
