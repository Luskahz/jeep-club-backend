package com.jeepclub.backend.authentication.core.application.service;

import com.jeepclub.backend.iam.authentication.core.application.result.PublicPasswordRecoveryResult;
import com.jeepclub.backend.iam.authentication.core.application.service.internal.PasswordRecoveryRequestManager;
import com.jeepclub.backend.iam.authentication.core.application.service.internal.PasswordResetTokenIssuer;
import com.jeepclub.backend.iam.authentication.core.application.service.internal.CredentialRevocationService;
import com.jeepclub.backend.iam.authentication.core.application.result.IssuedPasswordResetToken;
import com.jeepclub.backend.iam.authentication.core.domain.model.PasswordRecoveryRequest;
import com.jeepclub.backend.iam.authentication.core.port.NotificationPort;
import com.jeepclub.backend.iam.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.iam.authentication.core.port.RefreshTokenHashService;
import com.jeepclub.backend.iam.authentication.core.repository.PasswordRecoveryRequestRepository;
import com.jeepclub.backend.iam.authentication.core.application.service.passwordrecovery.PasswordRecoveryService;
import com.jeepclub.backend.iam.authentication.core.domain.enums.PasswordRecoveryRequestMethod;
import com.jeepclub.backend.iam.authentication.core.domain.enums.PasswordRecoveryRequestStatus;
import com.jeepclub.backend.iam.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.iam.identity.api.module.UserDetails;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestPasswordRecoveryServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-22T12:00:00Z");
    private static final String CPF = "52998224725";

    @Mock
    private AuthenticationAccountRepository accountRepository;
    @Mock
    private UserQuery identityQuery;
    @Mock
    private PasswordRecoveryRequestManager requestManager;
    @Mock private PasswordRecoveryRequestRepository requestRepository;
    @Mock private NotificationPort notificationPort;
    @Mock private PasswordResetTokenIssuer tokenIssuer;
    @Mock private RefreshTokenHashService tokenHashService;
    @Mock private PasswordHasher passwordHasher;
    @Mock private CredentialRevocationService revocationService;
    private PasswordRecoveryService service;
    private PublicPasswordRecoveryResult genericResult;

    @BeforeEach
    void setUp() {
        service = new PasswordRecoveryService(
                accountRepository,
                identityQuery,
                requestRepository,
                requestManager,
                notificationPort,
                tokenIssuer,
                tokenHashService,
                passwordHasher,
                revocationService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        genericResult = new PublicPasswordRecoveryResult(
                PasswordRecoveryRequestStatus.OPEN,
                PasswordRecoveryRequestMethod.UNDEFINED,
                NOW,
                NOW.plusSeconds(3600),
                null,
                null
        );
    }

    @Test
    void unknownCpfReturnsSamePublicRepresentationAsExistingUser() {
        when(requestManager.genericResult(NOW)).thenReturn(genericResult);
        when(identityQuery.findByCpf(CPF))
                .thenReturn(Optional.empty());

        PublicPasswordRecoveryResult unknown = service.request(CPF);

        when(identityQuery.findByCpf(CPF)).thenReturn(Optional.of(activeIdentity()));
        when(accountRepository.existsByIdentityId(1L)).thenReturn(true);
        PublicPasswordRecoveryResult existing = service.request(CPF);

        assertThat(unknown).isEqualTo(existing);
        verify(requestManager).getOrCreate(1L, NOW);
    }

    @Test
    void userWithoutEmailGetsGenericEmailResultWithoutTokenOrNotification() {
        UserDetails withoutEmail = new UserDetails(
                1L, "Lucas", null, null, CPF, null, null,
                null, true, NOW.minusSeconds(3600), null, null
        );
        when(identityQuery.findByCpf(CPF)).thenReturn(Optional.of(withoutEmail));
        when(accountRepository.existsByIdentityId(1L)).thenReturn(true);
        when(requestManager.genericEmailResult(NOW)).thenReturn(genericResult);

        assertThat(service.sendEmailToken(CPF)).isEqualTo(genericResult);
        verifyNoInteractions(tokenIssuer, notificationPort, requestRepository);
    }

    @Test
    void eligibleUserReceivesTokenThroughNotificationPort() {
        PasswordRecoveryRequest request = PasswordRecoveryRequest.createOpenRequest(
                1L, NOW, NOW.plusSeconds(3600)
        );
        IssuedPasswordResetToken token = new IssuedPasswordResetToken(
                "raw", "hash", "https://client/reset?token=raw"
        );
        when(identityQuery.findByCpf(CPF)).thenReturn(Optional.of(activeIdentity()));
        when(accountRepository.existsByIdentityId(1L)).thenReturn(true);
        when(requestManager.getOrCreate(1L, NOW)).thenReturn(request);
        when(tokenIssuer.issue()).thenReturn(token);
        when(requestManager.genericEmailResult(NOW)).thenReturn(genericResult);

        assertThat(service.sendEmailToken(CPF)).isEqualTo(genericResult);
        verify(requestRepository).save(request);
        verify(notificationPort).sendPasswordResetLink(
                "lucas@example.com", "https://client/reset?token=raw"
        );
    }

    private UserDetails activeIdentity() {
        return new UserDetails(
                1L, "Lucas", null, "lucas@example.com", CPF, null, null,
                null, true, NOW.minusSeconds(3600), null, null
        );
    }
}
