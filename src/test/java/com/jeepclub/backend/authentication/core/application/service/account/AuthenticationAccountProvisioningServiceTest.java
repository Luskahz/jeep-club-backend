package com.jeepclub.backend.authentication.core.application.service.account;

import com.jeepclub.backend.authentication.core.domain.enums.CredentialStatus;
import com.jeepclub.backend.authentication.core.domain.model.AuthenticationAccount;
import com.jeepclub.backend.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.identity.api.module.UserRegistration;
import com.jeepclub.backend.identity.api.module.UserRegistrationData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationAccountProvisioningServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private UserRegistration identityRegistration;

    @Mock
    private AuthenticationAccountRepository accountRepository;

    private AuthenticationAccountProvisioningService service;

    @BeforeEach
    void setUp() {
        service = new AuthenticationAccountProvisioningService(
                identityRegistration,
                accountRepository
        );
        when(identityRegistration.create(any())).thenReturn(42L);
        when(accountRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void provisionsPermanentAccountWithIdentityAssignedId() {
        Long identityId = service.provision(identityData(), "password-hash");

        AuthenticationAccount account = capturedAccount();
        assertThat(identityId).isEqualTo(42L);
        assertThat(account.getIdentityId()).isEqualTo(42L);
        assertThat(account.getCredentialStatus()).isEqualTo(CredentialStatus.PERMANENT);
        assertThat(account.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void provisionsPendingFirstAccessWithoutChangingIdentityData() {
        UserRegistrationData data = identityData();

        Long identityId = service.provisionPendingFirstAccess(data, "password-hash");

        AuthenticationAccount account = capturedAccount();
        assertThat(identityId).isEqualTo(42L);
        assertThat(account.getCredentialStatus())
                .isEqualTo(CredentialStatus.PENDING_FIRST_ACCESS);
        verify(identityRegistration).create(data);
    }

    private AuthenticationAccount capturedAccount() {
        ArgumentCaptor<AuthenticationAccount> captor =
                ArgumentCaptor.forClass(AuthenticationAccount.class);
        verify(accountRepository).create(captor.capture());
        return captor.getValue();
    }

    private UserRegistrationData identityData() {
        return new UserRegistrationData(
                "User Name",
                null,
                "identity@example.com",
                "52998224725",
                null,
                "5512999999999",
                null,
                NOW
        );
    }
}
