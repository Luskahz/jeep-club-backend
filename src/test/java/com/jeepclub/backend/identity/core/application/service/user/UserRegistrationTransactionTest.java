package com.jeepclub.backend.identity.core.application.service.user;

import com.jeepclub.backend.authentication.core.application.exceptions.account.AuthenticationAccountConflictException;
import com.jeepclub.backend.authentication.core.application.result.login.AuthenticatedLoginResult;
import com.jeepclub.backend.authentication.core.application.service.session.SessionService;
import com.jeepclub.backend.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.identity.api.module.UserQuery;
import com.jeepclub.backend.identity.api.module.UserRegistration;
import com.jeepclub.backend.identity.api.module.UserRegistrationData;
import com.jeepclub.backend.identity.api.module.exception.UserRegistrationConflictException;
import com.jeepclub.backend.identity.core.repository.UserRepository;
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
class UserRegistrationTransactionTest {

    private static final String CPF = "52998224725";

    @Autowired
    private UserRegistration userRegistration;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserQuery userQuery;

    @Autowired
    private SessionService sessionService;

    @MockitoSpyBean
    private AuthenticationAccountRepository accountRepository;

    @Test
    void rollsBackIdentityWhenAuthenticationAccountCreationFails() {
        doThrow(
                new AuthenticationAccountConflictException(
                        new IllegalStateException("simulated account conflict")
                )
        ).when(accountRepository).create(any());

        assertThatThrownBy(() -> userRegistration.createWithPermanentCredential(
                identityData(),
                "raw-password"
        )).isInstanceOf(UserRegistrationConflictException.class);

        assertThat(userRepository.existsByCpf(CPF)).isFalse();
    }

    @Test
    void commitsIdentityAndAuthenticationAccountWithTheSameId() {
        UserRegistrationData data = new UserRegistrationData(
                "Successful User",
                null,
                "successful@example.com",
                "16899535009",
                null,
                null,
                null,
                Instant.parse("2026-01-01T00:00:00Z")
        );

        Long identityId = userRegistration.createWithPermanentCredential(data, "raw-password");

        assertThat(userRepository.existsById(identityId)).isTrue();
        assertThat(accountRepository.existsByIdentityId(identityId)).isTrue();
    }

    @Test
    void canonicalizesFormattedCpfRejectsEquivalentDuplicateAndAcceptsAlternateLoginFormat() {
        UserRegistrationData formatted = new UserRegistrationData(
                "Formatted User", null, "formatted@example.com", "529.982.247-25",
                null, null, null, Instant.parse("2026-01-01T00:00:00Z")
        );

        userRegistration.registerAndAuthenticate(formatted, "Senha@123");

        assertThat(userQuery.findByCpf(CPF)).get().extracting(user -> user.cpf())
                .isEqualTo(CPF);
        assertThatThrownBy(() -> userRegistration.createWithPermanentCredential(
                new UserRegistrationData(
                        "Duplicate User", null, "duplicate@example.com", CPF,
                        null, null, null, Instant.parse("2026-01-01T00:01:00Z")
                ),
                "Senha@456"
        )).isInstanceOf(UserRegistrationConflictException.class);
        assertThat(sessionService.login(CPF, "Senha@123"))
                .isInstanceOf(AuthenticatedLoginResult.class);
    }

    private UserRegistrationData identityData() {
        return new UserRegistrationData(
                "Transactional User",
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
