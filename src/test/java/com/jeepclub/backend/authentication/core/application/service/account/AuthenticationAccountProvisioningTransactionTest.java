package com.jeepclub.backend.authentication.core.application.service.account;

import com.jeepclub.backend.authentication.core.application.exceptions.account.AuthenticationAccountConflictException;
import com.jeepclub.backend.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.identity.api.module.IdentityRegistrationData;
import com.jeepclub.backend.identity.core.repository.IdentityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
class AuthenticationAccountProvisioningTransactionTest {

    private static final String CPF = "52998224725";

    @Autowired
    private AuthenticationAccountProvisioningService service;

    @Autowired
    private IdentityRepository identityRepository;

    @MockitoSpyBean
    private AuthenticationAccountRepository accountRepository;

    @Test
    void rollsBackIdentityWhenAuthenticationAccountCreationFails() {
        doThrow(
                new AuthenticationAccountConflictException(
                        new IllegalStateException("simulated account conflict")
                )
        ).when(accountRepository).create(any());

        assertThatThrownBy(() -> service.provision(identityData(), "password-hash"))
                .isInstanceOf(AuthenticationAccountConflictException.class);

        assertThat(identityRepository.existsByCpf(CPF)).isFalse();
    }

    @Test
    void commitsIdentityAndAuthenticationAccountWithTheSameId() {
        IdentityRegistrationData data = new IdentityRegistrationData(
                "Successful Identity",
                null,
                "successful@example.com",
                "16899535009",
                null,
                null,
                null,
                Instant.parse("2026-01-01T00:00:00Z")
        );

        Long identityId = service.provision(data, "password-hash");

        assertThat(identityRepository.existsById(identityId)).isTrue();
        assertThat(accountRepository.existsByIdentityId(identityId)).isTrue();
    }

    private IdentityRegistrationData identityData() {
        return new IdentityRegistrationData(
                "Transactional Identity",
                null,
                "transactional@example.com",
                CPF,
                null,
                null,
                null,
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }
}
