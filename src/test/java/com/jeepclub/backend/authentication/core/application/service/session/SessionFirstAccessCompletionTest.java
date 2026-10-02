package com.jeepclub.backend.authentication.core.application.service.session;

import com.jeepclub.backend.iam.authentication.core.application.result.AuthTokens;
import com.jeepclub.backend.iam.authentication.core.application.service.internal.CredentialRevocationService;
import com.jeepclub.backend.iam.authentication.core.application.service.internal.PasswordChangeChallengeIssuer;
import com.jeepclub.backend.iam.authentication.core.application.service.internal.TokenIssuanceService;
import com.jeepclub.backend.iam.authentication.core.application.service.session.SessionService;
import com.jeepclub.backend.iam.authentication.core.domain.enums.CredentialStatus;
import com.jeepclub.backend.iam.authentication.core.domain.model.AuthenticationAccount;
import com.jeepclub.backend.iam.authentication.core.domain.model.PasswordChangeChallenge;
import com.jeepclub.backend.iam.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.iam.authentication.core.port.RefreshTokenHashService;
import com.jeepclub.backend.iam.authentication.core.repository.*;
import com.jeepclub.backend.iam.identity.api.module.UserDetails;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import com.jeepclub.backend.memberships.api.module.MembershipOnboardingCompletion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionFirstAccessCompletionTest {
    private static final Instant NOW = Instant.parse("2026-09-19T12:00:00Z");
    @Mock AuthenticationAccountRepository accountRepository;
    @Mock SessionRepository sessionRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordChangeChallengeRepository challengeRepository;
    @Mock PasswordRecoveryRequestRepository recoveryRequestRepository;
    @Mock PasswordHasher passwordHasher;
    @Mock RefreshTokenHashService tokenHashService;
    @Mock CredentialRevocationService credentialRevocationService;
    @Mock PasswordChangeChallengeIssuer challengeIssuer;
    @Mock TokenIssuanceService tokenIssuanceService;
    @Mock UserQuery userQuery;
    @Mock MembershipOnboardingCompletion membershipOnboardingCompletion;
    @Mock Clock clock;
    @InjectMocks SessionService service;

    @Test
    void temporaryPasswordCompletionMakesCredentialPermanentAndCompletesMembership() {
        AuthenticationAccount account = AuthenticationAccount.createPendingFirstAccess(
                42L, "old-hash", NOW.minusSeconds(3600)
        );
        PasswordChangeChallenge challenge = PasswordChangeChallenge.create(
                42L, "challenge-hash", NOW.minusSeconds(60), NOW.plusSeconds(600)
        );
        AuthTokens tokens = new AuthTokens("refresh", "access", 3600);
        when(clock.instant()).thenReturn(NOW);
        when(tokenHashService.hash("challenge")).thenReturn("challenge-hash");
        when(challengeRepository.findUserIdByTokenHash("challenge-hash"))
                .thenReturn(Optional.of(42L));
        when(accountRepository.findByIdentityIdForUpdate(42L)).thenReturn(Optional.of(account));
        when(challengeRepository.findByTokenHashForUpdate("challenge-hash"))
                .thenReturn(Optional.of(challenge));
        when(passwordHasher.hash("new-password")).thenReturn("new-hash");
        when(userQuery.findById(42L)).thenReturn(Optional.of(new UserDetails(
                42L, "Member", null, null, "52998224725", null, null, null,
                true, NOW.minusSeconds(3600), null, null
        )));
        when(tokenIssuanceService.issue(account, "Member", NOW)).thenReturn(tokens);

        assertThat(service.completeRequiredPasswordChange("challenge", "new-password"))
                .isSameAs(tokens);
        assertThat(account.getCredentialStatus()).isEqualTo(CredentialStatus.PERMANENT);
        assertThat(challenge.isUsed()).isTrue();
        verify(membershipOnboardingCompletion)
                .completeApprovedApplicationForIdentity(42L, NOW);
    }
}
