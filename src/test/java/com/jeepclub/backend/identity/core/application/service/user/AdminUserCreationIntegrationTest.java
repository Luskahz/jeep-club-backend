package com.jeepclub.backend.identity.core.application.service.user;

import com.jeepclub.backend.iam.authentication.core.application.exceptions.account.ActivationInvitationException;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.account.AuthenticationAccountConflictException;
import com.jeepclub.backend.iam.authentication.core.application.result.login.PasswordChangeRequiredLoginResult;
import com.jeepclub.backend.iam.authentication.core.application.service.activation.AccountActivationInvitationService;
import com.jeepclub.backend.iam.authentication.core.application.service.session.SessionService;
import com.jeepclub.backend.iam.authentication.core.domain.enums.CredentialStatus;
import com.jeepclub.backend.iam.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.iam.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.iam.authentication.infra.mail.AccountActivationMailSender;
import com.jeepclub.backend.iam.authentication.infra.persistence.jpa.AccountActivationInvitationJpaRepository;
import com.jeepclub.backend.iam.identity.api.module.AdminUserCreationResult;
import com.jeepclub.backend.iam.identity.api.module.UserActivationMethod;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import com.jeepclub.backend.iam.identity.api.module.UserRegistration;
import com.jeepclub.backend.iam.identity.api.module.UserRegistrationData;
import com.jeepclub.backend.iam.identity.api.module.exception.UserRegistrationConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import org.mockito.ArgumentCaptor;

@SpringBootTest
@ActiveProfiles("test")
class AdminUserCreationIntegrationTest {
    @Autowired private UserRegistration registration;
    @Autowired private UserQuery users;
    @MockitoSpyBean private AuthenticationAccountRepository accounts;
    @Autowired private PasswordHasher hasher;
    @Autowired private SessionService sessions;
    @Autowired private AccountActivationInvitationService invitations;
    @Autowired private AccountActivationInvitationJpaRepository invitationRepository;
    @Autowired private Clock clock;
    @MockitoBean private AccountActivationMailSender mailSender;

    @Test
    void temporaryPasswordCreatesPendingAccountAndRequiresPasswordChange() {
        AdminUserCreationResult result = registration.createAdministratively(
                data("52998224725", null), UserActivationMethod.TEMPORARY_PASSWORD);
        assertThat(result.temporaryPassword()).isNotBlank();
        assertThat(result.invitationStatus()).isNull();
        assertThat(users.findById(result.userId())).get().satisfies(user -> {
            assertThat(user.birthDate()).isEqualTo(LocalDate.of(1990, 4, 12));
            assertThat(user.rg()).isEqualTo("52998224725");
            assertThat(user.phoneNumber()).isEqualTo("11999998888");
        });
        var account = accounts.findByIdentityId(result.userId()).orElseThrow();
        assertThat(account.getCredentialStatus()).isEqualTo(CredentialStatus.PENDING_FIRST_ACCESS);
        assertThat(account.getPasswordHash()).isNotEqualTo(result.temporaryPassword());
        assertThat(hasher.matches(result.temporaryPassword(), account.getPasswordHash())).isTrue();
        var login = sessions.login("52998224725", result.temporaryPassword());
        assertThat(login).isInstanceOf(PasswordChangeRequiredLoginResult.class);
        sessions.completeRequiredPasswordChange(
                ((PasswordChangeRequiredLoginResult) login).passwordChangeToken(), "Definitiva@123");
        assertThat(accounts.findByIdentityId(result.userId()).orElseThrow().getCredentialStatus())
                .isEqualTo(CredentialStatus.PERMANENT);
    }

    @Test
    void invitationUsesHashedTokenRotatesAndIsSingleUse() {
        AdminUserCreationResult result = registration.createAdministratively(
                data("16899535009", " ADMIN@example.com "), UserActivationMethod.EMAIL_INVITATION);
        assertThat(result.temporaryPassword()).isNull();
        assertThat(result.invitationStatus()).isEqualTo("QUEUED");
        ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
        verify(mailSender).send(anyString(), anyString(), sent.capture());
        String first = sent.getValue();
        assertThat(invitationRepository.findById(result.userId()).orElseThrow().getTokenHash()).isNotEqualTo(first);
        assertThat(users.findById(result.userId())).get().extracting(user -> user.email())
                .isEqualTo("admin@example.com");
        invitations.validate(first);
        invitations.validate(first);
        invitations.issueForExistingUser(result.userId());
        verify(mailSender, atLeastOnce()).send(anyString(), anyString(), sent.capture());
        String second = sent.getAllValues().get(sent.getAllValues().size() - 1);
        assertThat(second).isNotEqualTo(first);
        assertThatThrownBy(() -> invitations.validate(first)).isInstanceOf(ActivationInvitationException.class);
        invitations.complete(second, "Definitiva@456");
        assertThat(accounts.findByIdentityId(result.userId()).orElseThrow().getCredentialStatus())
                .isEqualTo(CredentialStatus.PERMANENT);
        assertThatThrownBy(() -> invitations.complete(second, "Definitiva@789"))
                .isInstanceOf(ActivationInvitationException.class);
        assertThatThrownBy(() -> invitations.issueForExistingUser(result.userId()))
                .isInstanceOf(ActivationInvitationException.class);
    }

    @Test
    void duplicateCpfDoesNotCreateSecondAccount() {
        registration.createAdministratively(data("11144477735", null), UserActivationMethod.TEMPORARY_PASSWORD);
        assertThatThrownBy(() -> registration.createAdministratively(data("11144477735", null),
                UserActivationMethod.TEMPORARY_PASSWORD)).isInstanceOf(UserRegistrationConflictException.class);
    }

    @Test
    void provisioningFailureRollsBackIdentity() {
        doThrow(new AuthenticationAccountConflictException(new IllegalStateException("simulated")))
                .when(accounts).create(org.mockito.ArgumentMatchers.any());
        assertThatThrownBy(() -> registration.createAdministratively(
                data("58392174097", null), UserActivationMethod.TEMPORARY_PASSWORD))
                .isInstanceOf(UserRegistrationConflictException.class);
        assertThat(users.existsByCpf("58392174097")).isFalse();
    }

    @Test
    void deliveryFailureCanBeRecoveredByReissue() {
        doThrow(new org.springframework.mail.MailSendException("simulated"))
                .doNothing().when(mailSender).send(anyString(), anyString(), anyString());
        var result = registration.createAdministratively(
                data("12763845053", "delivery@example.com"), UserActivationMethod.EMAIL_INVITATION);
        assertThat(invitations.deliveryStatus(result.userId())).isEqualTo("DELIVERY_FAILED");
        invitations.issueForExistingUser(result.userId());
        assertThat(invitations.deliveryStatus(result.userId())).isEqualTo("SENT");
    }

    @Test
    void simultaneousActivationConsumesInvitationOnlyOnce() throws Exception {
        var result = registration.createAdministratively(
                data("65923017407", "concurrent@example.com"), UserActivationMethod.EMAIL_INVITATION);
        ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
        verify(mailSender).send(anyString(), anyString(), sent.capture());
        String token = sent.getValue();
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> completeAfter(start, token));
            var second = executor.submit(() -> completeAfter(start, token));
            start.countDown();
            Throwable firstFailure = first.get(15, TimeUnit.SECONDS);
            Throwable secondFailure = second.get(15, TimeUnit.SECONDS);
            assertThat((firstFailure == null ? 1 : 0) + (secondFailure == null ? 1 : 0)).isEqualTo(1);
            assertThat(firstFailure == null ? secondFailure : firstFailure)
                    .isInstanceOf(ActivationInvitationException.class);
            assertThat(accounts.findByIdentityId(result.userId()).orElseThrow().getCredentialStatus())
                    .isEqualTo(CredentialStatus.PERMANENT);
        } finally {
            executor.shutdownNow();
        }
    }

    private Throwable completeAfter(CountDownLatch start, String token) {
        try {
            start.await();
            invitations.complete(token, "Definitiva@789");
            return null;
        } catch (Throwable failure) {
            return failure;
        }
    }

    private UserRegistrationData data(String cpf, String email) {
        return new UserRegistrationData("Admin User", LocalDate.of(1990, 4, 12), email, cpf,
                cpf, "(11) 99999-8888", null, Instant.now(clock));
    }
}
