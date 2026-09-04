package com.jeepclub.backend.authentication.core.application.service;

import com.jeepclub.backend.authentication.core.application.result.PublicPasswordRecoveryResult;
import com.jeepclub.backend.authentication.core.application.service.internal.PasswordRecoveryRequestManager;
import com.jeepclub.backend.authentication.core.application.service.passwordrecovery.PasswordRecoveryService;
import com.jeepclub.backend.authentication.core.domain.enums.PasswordRecoveryRequestMethod;
import com.jeepclub.backend.authentication.core.domain.enums.PasswordRecoveryRequestStatus;
import com.jeepclub.backend.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.identity.api.module.UserDetails;
import com.jeepclub.backend.identity.api.module.UserQuery;
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
    private PasswordRecoveryService service;
    private PublicPasswordRecoveryResult genericResult;

    @BeforeEach
    void setUp() {
        service = new PasswordRecoveryService(
                accountRepository,
                identityQuery,
                null,
                requestManager,
                null,
                null,
                null,
                null,
                null,
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
        when(requestManager.genericResult(NOW)).thenReturn(genericResult);
    }

    @Test
    void unknownCpfReturnsSamePublicRepresentationAsExistingUser() {
        when(identityQuery.findByCpf(CPF))
                .thenReturn(Optional.empty());

        PublicPasswordRecoveryResult unknown = service.request(CPF);

        when(identityQuery.findByCpf(CPF)).thenReturn(Optional.of(activeIdentity()));
        when(accountRepository.existsByIdentityId(1L)).thenReturn(true);
        PublicPasswordRecoveryResult existing = service.request(CPF);

        assertThat(unknown).isEqualTo(existing);
        verify(requestManager).getOrCreate(1L, NOW);
    }

    private UserDetails activeIdentity() {
        return new UserDetails(
                1L, "Lucas", null, "lucas@example.com", CPF, null, null,
                null, true, NOW.minusSeconds(3600), null, null
        );
    }
}
