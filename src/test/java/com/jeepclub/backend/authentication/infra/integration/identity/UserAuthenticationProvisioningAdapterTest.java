package com.jeepclub.backend.authentication.infra.integration.identity;

import com.jeepclub.backend.iam.authentication.core.application.service.internal.TokenIssuanceService;
import com.jeepclub.backend.iam.authentication.core.domain.enums.CredentialStatus;
import com.jeepclub.backend.iam.authentication.core.domain.model.AuthenticationAccount;
import com.jeepclub.backend.iam.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.iam.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.iam.authentication.infra.integration.identity.UserAuthenticationProvisioningAdapter;
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
class UserAuthenticationProvisioningAdapterTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock private AuthenticationAccountRepository accountRepository;
    @Mock private PasswordHasher passwordHasher;
    @Mock private TokenIssuanceService tokenIssuanceService;

    private UserAuthenticationProvisioningAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new UserAuthenticationProvisioningAdapter(
                accountRepository,
                passwordHasher,
                tokenIssuanceService
        );
        when(passwordHasher.hash("raw-password")).thenReturn("password-hash");
        when(accountRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void provisionsPermanentAuthenticationForExistingUserId() {
        adapter.provisionPermanent(42L, "raw-password", NOW);
        AuthenticationAccount account = capturedAccount();
        assertThat(account.getIdentityId()).isEqualTo(42L);
        assertThat(account.getCredentialStatus()).isEqualTo(CredentialStatus.PERMANENT);
        assertThat(account.getPasswordHash()).isEqualTo("password-hash");
    }

    @Test
    void provisionsPendingFirstAccessWithoutOwningUserCreation() {
        adapter.provisionPendingFirstAccess(42L, "raw-password", NOW);
        AuthenticationAccount account = capturedAccount();
        assertThat(account.getIdentityId()).isEqualTo(42L);
        assertThat(account.getCredentialStatus()).isEqualTo(CredentialStatus.PENDING_FIRST_ACCESS);
    }

    private AuthenticationAccount capturedAccount() {
        ArgumentCaptor<AuthenticationAccount> captor = ArgumentCaptor.forClass(AuthenticationAccount.class);
        verify(accountRepository).create(captor.capture());
        return captor.getValue();
    }
}
