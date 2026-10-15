package com.jeepclub.backend.authentication.core.application.service.session;

import com.jeepclub.backend.iam.authentication.core.application.service.session.SessionService;
import com.jeepclub.backend.iam.authentication.core.application.service.internal.*;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.login.PasswordChangeChallengeInvalidException;
import com.jeepclub.backend.iam.authentication.core.domain.model.*;
import com.jeepclub.backend.iam.authentication.core.port.*;
import com.jeepclub.backend.iam.authentication.core.repository.*;
import com.jeepclub.backend.iam.identity.api.module.*;
import com.jeepclub.backend.memberships.api.module.MembershipOnboardingCompletion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionChallengeLockExpirationTest {
    private static final Instant NOW=Instant.parse("2026-10-14T12:00:00Z");
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
    @Mock UserIdentityLock identityLock;
    @Mock MembershipOnboardingCompletion membershipOnboardingCompletion;
    @Mock Clock clock;
    @InjectMocks SessionService service;

    @Test void expiredWhileWaitingForIdentityLockMustNotReachPasswordMutation() {
        AtomicReference<Instant> current=new AtomicReference<>(NOW);
        when(clock.instant()).thenAnswer(call -> current.get());
        when(tokenHashService.hash("raw")).thenReturn("hash");
        when(challengeRepository.findUserIdByTokenHash("hash")).thenReturn(Optional.of(42L));
        when(identityLock.byId(42L)).thenAnswer(call -> {
            current.set(NOW.plusSeconds(120));
            return Optional.of(new UserDetails(42L,"Name",null,null,"52998224725",null,null,null,true,NOW.minusSeconds(3600),null,null));
        });
        AuthenticationAccount account=AuthenticationAccount.createPendingFirstAccess(42L,"old-hash",NOW.minusSeconds(3600));
        PasswordChangeChallenge challenge=PasswordChangeChallenge.create(42L,"hash",NOW.minusSeconds(30),NOW.plusSeconds(60));
        when(accountRepository.findByIdentityIdForUpdate(42L)).thenReturn(Optional.of(account));
        when(challengeRepository.findByTokenHashForUpdate("hash")).thenReturn(Optional.of(challenge));
        assertThatThrownBy(() -> service.completeRequiredPasswordChange("raw","new-password"))
                .isInstanceOf(PasswordChangeChallengeInvalidException.class);
        verifyNoInteractions(passwordHasher, tokenIssuanceService, credentialRevocationService, membershipOnboardingCompletion);
        assertThat(challenge.isUsed()).isFalse();
        assertThat(account.getPasswordHash()).isEqualTo("old-hash");
    }
}
