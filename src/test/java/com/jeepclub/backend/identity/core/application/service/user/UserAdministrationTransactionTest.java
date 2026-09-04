package com.jeepclub.backend.identity.core.application.service.user;

import com.jeepclub.backend.authentication.core.application.service.internal.CredentialRevocationService;
import com.jeepclub.backend.authentication.core.domain.enums.AuthenticationAccessStatus;
import com.jeepclub.backend.authentication.core.domain.enums.AuthenticationStatus;
import com.jeepclub.backend.authentication.core.domain.enums.CredentialStatus;
import com.jeepclub.backend.authentication.core.domain.model.AuthenticationAccount;
import com.jeepclub.backend.authentication.core.domain.model.PasswordChangeChallenge;
import com.jeepclub.backend.authentication.core.domain.model.RefreshToken;
import com.jeepclub.backend.authentication.core.domain.model.Session;
import com.jeepclub.backend.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.authentication.core.repository.PasswordChangeChallengeRepository;
import com.jeepclub.backend.authentication.core.repository.RefreshTokenRepository;
import com.jeepclub.backend.authentication.core.repository.SessionRepository;
import com.jeepclub.backend.identity.api.module.UserAdministration;
import com.jeepclub.backend.identity.api.module.UserQuery;
import com.jeepclub.backend.identity.api.module.UserRegistration;
import com.jeepclub.backend.identity.api.module.UserRegistrationData;
import com.jeepclub.backend.identity.api.module.spi.UserAuthenticationAdministrationPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@SpringBootTest
@ActiveProfiles("test")
class UserAdministrationTransactionTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-01T12:00:00Z");
    private static final Instant CHANGED_AT = CREATED_AT.plusSeconds(60);

    @Autowired private UserRegistration userRegistration;
    @Autowired private UserAdministration identityAdministration;
    @Autowired private UserQuery identityQuery;
    @Autowired private AuthenticationAccountRepository accountRepository;
    @Autowired private SessionRepository sessionRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordChangeChallengeRepository challengeRepository;

    @MockitoSpyBean
    private UserAuthenticationAdministrationPort authenticationAdministrationPort;

    @MockitoSpyBean
    private CredentialRevocationService credentialRevocationService;

    @AfterEach
    void resetSpies() {
        reset(authenticationAdministrationPort, credentialRevocationService);
    }

    @Test
    void rollsBackIdentityWhenAuthenticationDisableFailsBeforeCompletion() {
        Long identityId = provision("39053344705", "before-auth@example.com");
        AuthenticationArtifacts artifacts = createActiveArtifacts(identityId, "before-auth");
        doThrow(new ControlledFailure()).when(authenticationAdministrationPort)
                .disableAuthentication(anyLong(), any(Instant.class));

        assertThatThrownBy(() -> identityAdministration.disable(identityId, CHANGED_AT))
                .isInstanceOf(ControlledFailure.class);

        assertActive(identityId);
        assertArtifactsRemainActive(artifacts);
    }

    @Test
    void rollsBackBothAggregatesAndCredentialRevocationsWhenRevocationFails() {
        Long identityId = provision("11144477735", "revocation@example.com");
        AuthenticationArtifacts artifacts = createActiveArtifacts(identityId, "revocation");
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new ControlledFailure();
        }).when(credentialRevocationService).revokeAllForUser(anyLong(), any(Instant.class));

        assertThatThrownBy(() -> identityAdministration.disable(identityId, CHANGED_AT))
                .isInstanceOf(ControlledFailure.class);

        assertActive(identityId);
        assertArtifactsRemainActive(artifacts);
    }

    @Test
    void rollsBackBothAggregatesWhenAuthenticationEnableFails() {
        Long identityId = provision("16899535009", "enable@example.com");
        identityAdministration.disable(identityId, CHANGED_AT);
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new ControlledFailure();
        }).when(authenticationAdministrationPort)
                .enableAuthentication(anyLong(), any(Instant.class));

        assertThatThrownBy(() -> identityAdministration.enable(
                identityId,
                CHANGED_AT.plusSeconds(60)
        )).isInstanceOf(ControlledFailure.class);

        assertThat(identityQuery.isAdministrativelyActive(identityId)).isFalse();
        assertThat(account(identityId).getAccessStatus())
                .isEqualTo(AuthenticationAccessStatus.DISABLED);
    }

    @Test
    void disableAndEnablePreserveLockCredentialAndPasswordState() {
        Long identityId = userRegistration.createPendingFirstAccess(
                identityData("52998224725", "preserved-state@example.com"),
                "preserved-password"
        );
        AuthenticationAccount account = account(identityId);
        String preservedHash = account.getPasswordHash();
        for (int attempt = 0; attempt < 5; attempt++) {
            account.registerFailedLogin();
        }
        accountRepository.save(account);

        identityAdministration.disable(identityId, CHANGED_AT);
        identityAdministration.enable(identityId, CHANGED_AT.plusSeconds(60));

        AuthenticationAccount reloaded = account(identityId);
        assertThat(identityQuery.isAdministrativelyActive(identityId)).isTrue();
        assertThat(reloaded.getAccessStatus()).isEqualTo(AuthenticationAccessStatus.ENABLED);
        assertThat(reloaded.getAuthenticationStatus()).isEqualTo(AuthenticationStatus.LOCKED);
        assertThat(reloaded.getCredentialStatus()).isEqualTo(CredentialStatus.PENDING_FIRST_ACCESS);
        assertThat(reloaded.getPasswordHash()).isEqualTo(preservedHash);
        assertThat(reloaded.getFailedLoginAttempts()).isEqualTo(5);
    }

    private Long provision(String cpf, String email) {
        return userRegistration.createWithPermanentCredential(identityData(cpf, email), "password-raw");
    }

    private UserRegistrationData identityData(String cpf, String email) {
        return new UserRegistrationData(
                "Transactional Lifecycle",
                null,
                email,
                cpf,
                null,
                null,
                null,
                CREATED_AT
        );
    }

    private AuthenticationArtifacts createActiveArtifacts(Long identityId, String suffix) {
        Session session = sessionRepository.save(Session.create(
                identityId,
                Duration.ofHours(1),
                CREATED_AT
        ));
        RefreshToken refreshToken = refreshTokenRepository.save(RefreshToken.create(
                session,
                "refresh-token-" + suffix,
                Duration.ofHours(1),
                CREATED_AT
        ));
        PasswordChangeChallenge challenge = challengeRepository.save(
                PasswordChangeChallenge.create(
                        identityId,
                        "challenge-token-" + suffix,
                        CREATED_AT,
                        CREATED_AT.plus(Duration.ofHours(1))
                )
        );
        return new AuthenticationArtifacts(session.getId(), refreshToken.getId(), challenge.getTokenHash());
    }

    private void assertActive(Long identityId) {
        assertThat(identityQuery.isAdministrativelyActive(identityId)).isTrue();
        assertThat(account(identityId).getAccessStatus())
                .isEqualTo(AuthenticationAccessStatus.ENABLED);
    }

    private void assertArtifactsRemainActive(AuthenticationArtifacts artifacts) {
        Instant reference = CHANGED_AT.plusSeconds(1);
        assertThat(sessionRepository.findById(artifacts.sessionId()).orElseThrow().isActive(reference))
                .isTrue();
        assertThat(refreshTokenRepository.findById(artifacts.refreshTokenId()).orElseThrow().isActive(reference))
                .isTrue();
        assertThat(challengeRepository.findByTokenHash(artifacts.challengeTokenHash())
                .orElseThrow().isValid(reference)).isTrue();
    }

    private AuthenticationAccount account(Long identityId) {
        return accountRepository.findByIdentityId(identityId).orElseThrow();
    }

    private record AuthenticationArtifacts(
            Long sessionId,
            Long refreshTokenId,
            String challengeTokenHash
    ) {
    }

    private static final class ControlledFailure extends RuntimeException {
    }
}
