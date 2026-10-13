package com.jeepclub.backend.identity.core.application.service.user;

import com.jeepclub.backend.iam.authentication.core.application.exceptions.account.ActivationInvitationException;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.account.AuthenticationAccountConflictException;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.login.InvalidCredentialsException;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.login.PasswordChangeChallengeInvalidException;
import com.jeepclub.backend.iam.authentication.core.application.result.login.PasswordChangeRequiredLoginResult;
import com.jeepclub.backend.iam.authentication.core.application.service.activation.AccountActivationInvitationService;
import com.jeepclub.backend.iam.authentication.core.application.service.session.SessionService;
import com.jeepclub.backend.iam.authentication.core.domain.enums.CredentialStatus;
import com.jeepclub.backend.iam.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.iam.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.iam.authentication.infra.mail.AccountActivationMailSender;
import com.jeepclub.backend.iam.authentication.infra.mail.AccountActivationDeliveryStatusWriter;
import com.jeepclub.backend.iam.authentication.infra.persistence.jpa.AccountActivationInvitationJpaRepository;
import com.jeepclub.backend.iam.authentication.infra.persistence.jpa.PasswordChangeChallengeJpaRepository;
import com.jeepclub.backend.iam.authorization.core.repository.UserRoleRepository;
import com.jeepclub.backend.iam.identity.api.module.AdminUserCreationResult;
import com.jeepclub.backend.iam.identity.api.module.UserActivationMethod;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import com.jeepclub.backend.iam.identity.api.module.UserRegistration;
import com.jeepclub.backend.iam.identity.api.module.UserRegistrationData;
import com.jeepclub.backend.iam.identity.api.module.exception.UserRegistrationConflictException;
import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;
import com.jeepclub.backend.memberships.core.repository.MembershipApplicationRepository;
import com.jeepclub.backend.platform.logging.SystemLogJpaRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
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
    @Autowired private MembershipApplicationRepository membershipApplications;
    @Autowired private AccountActivationDeliveryStatusWriter statusWriter;
    @Autowired private SystemLogJpaRepository systemLogs;
    @Autowired private PasswordChangeChallengeJpaRepository challenges;
    @Autowired private UserRoleRepository userRoles;
    @MockitoBean private AccountActivationMailSender mailSender;

    @Test
    void temporaryPasswordCreatesPendingAccountAndRequiresPasswordChange() {
        AdminUserCreationResult result = registration.createAdministratively(
                data("52998224725", null), UserActivationMethod.TEMPORARY_PASSWORD, 1L);
        assertThat(result.temporaryPassword()).isNotBlank();
        assertThat(users.findById(result.userId())).get().satisfies(user -> {
            assertThat(user.birthDate()).isEqualTo(LocalDate.of(1990, 4, 12));
            assertThat(user.rg()).isEqualTo("52998224725");
            assertThat(user.phoneNumber()).isEqualTo("11999998888");
        });
        var account = accounts.findByIdentityId(result.userId()).orElseThrow();
        assertThat(account.getCredentialStatus()).isEqualTo(CredentialStatus.PENDING_FIRST_ACCESS);
        assertThat(account.getPasswordHash()).isNotEqualTo(result.temporaryPassword());
        assertThat(hasher.matches(result.temporaryPassword(), account.getPasswordHash())).isTrue();
        assertThat(systemLogs.findAll()).anySatisfy(log -> {
            assertThat(log.getActorId()).isEqualTo(1L);
            assertThat(log.getAction()).isEqualTo("IDENTITY_USER_CREATE_TEMPORARY_PASSWORD");
            assertThat(log.getPath()).isEqualTo("/identity/admin/users/" + result.userId());
        });
        var login = sessions.login("52998224725", result.temporaryPassword());
        assertThat(login).isInstanceOf(PasswordChangeRequiredLoginResult.class);
        String challenge = ((PasswordChangeRequiredLoginResult) login).passwordChangeToken();
        sessions.completeRequiredPasswordChange(challenge, "Definitiva@123");
        assertThat(accounts.findByIdentityId(result.userId()).orElseThrow().getCredentialStatus())
                .isEqualTo(CredentialStatus.PERMANENT);
        assertThat(userRoles.findRolesByUserId(result.userId())).isEmpty();
        assertThatThrownBy(() -> sessions.completeRequiredPasswordChange(challenge, "Outra@123"))
                .isInstanceOf(PasswordChangeChallengeInvalidException.class);
        assertThatThrownBy(() -> sessions.login("52998224725", result.temporaryPassword()))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void expiredPasswordChangeChallengeCannotBeCompleted() {
        var result = registration.createAdministratively(data("67420985371", null),
                UserActivationMethod.TEMPORARY_PASSWORD, 1L);
        var login = (PasswordChangeRequiredLoginResult) sessions.login("67420985371", result.temporaryPassword());
        var challenge = challenges.findAll().stream()
                .filter(item -> item.getUserId().equals(result.userId()) && !item.isUsed())
                .findFirst().orElseThrow();
        challenge.setCreatedAt(challenge.getCreatedAt().minusSeconds(2));
        challenge.setExpiresAt(challenge.getCreatedAt().plusSeconds(1));
        challenges.saveAndFlush(challenge);
        assertThatThrownBy(() -> sessions.completeRequiredPasswordChange(
                login.passwordChangeToken(), "Definitiva@123"))
                .isInstanceOf(PasswordChangeChallengeInvalidException.class);
        assertThat(accounts.findByIdentityId(result.userId()).orElseThrow().getCredentialStatus())
                .isEqualTo(CredentialStatus.PENDING_FIRST_ACCESS);
    }

    @Test
    void invitationUsesHashedTokenRotatesAndIsSingleUse() {
        AdminUserCreationResult result = registration.createAdministratively(
                data("16899535009", " ADMIN@example.com "), UserActivationMethod.EMAIL_INVITATION, 1L);
        assertThat(result.temporaryPassword()).isNull();
        assertThat(registration.administrativeInvitationStatus(result.userId())).isEqualTo("SENT");
        ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
        verify(mailSender).send(anyString(), anyString(), sent.capture());
        String first = sent.getValue();
        assertThat(invitationRepository.findById(result.userId()).orElseThrow().getTokenHash()).isNotEqualTo(first);
        assertThat(users.findById(result.userId())).get().extracting(user -> user.email())
                .isEqualTo("admin@example.com");
        assertThat(userRoles.findRolesByUserId(result.userId())).isEmpty();
        invitations.validate(first);
        invitations.validate(first);
        invitations.issueForExistingUser(result.userId(), 1L);
        verify(mailSender, atLeastOnce()).send(anyString(), anyString(), sent.capture());
        String second = sent.getAllValues().get(sent.getAllValues().size() - 1);
        assertThat(second).isNotEqualTo(first);
        assertThatThrownBy(() -> invitations.validate(first)).isInstanceOf(ActivationInvitationException.class);
        invitations.complete(second, "Definitiva@456");
        assertThat(accounts.findByIdentityId(result.userId()).orElseThrow().getCredentialStatus())
                .isEqualTo(CredentialStatus.PERMANENT);
        assertThatThrownBy(() -> invitations.complete(second, "Definitiva@789"))
                .isInstanceOf(ActivationInvitationException.class);
        assertThatThrownBy(() -> invitations.issueForExistingUser(result.userId(), 1L))
                .isInstanceOf(ActivationInvitationException.class);
    }

    @Test
    void administrativeTemporaryPasswordAccountCanSwitchToInvitationWithoutRecreation() {
        var result = registration.createAdministratively(data("71835294061", "switch@example.com"),
                UserActivationMethod.TEMPORARY_PASSWORD, 1L);
        invitations.issueForExistingUser(result.userId(), 1L);
        ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
        verify(mailSender).send(anyString(), anyString(), sent.capture());
        assertThat(users.findById(result.userId())).isPresent();
        assertThat(accounts.existsByIdentityId(result.userId())).isTrue();
        assertThat(invitationRepository.findById(result.userId())).isPresent();
        invitations.complete(sent.getValue(), "Definitiva@123");
        assertThat(accounts.findByIdentityId(result.userId()).orElseThrow().getCredentialStatus())
                .isEqualTo(CredentialStatus.PERMANENT);
        assertThatThrownBy(() -> sessions.login("71835294061", result.temporaryPassword()))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void duplicateCpfDoesNotCreateSecondAccount() {
        registration.createAdministratively(data("11144477735", null), UserActivationMethod.TEMPORARY_PASSWORD, 1L);
        assertThatThrownBy(() -> registration.createAdministratively(data("11144477735", null),
                UserActivationMethod.TEMPORARY_PASSWORD, 1L)).isInstanceOf(UserRegistrationConflictException.class);
    }

    @Test
    void provisioningFailureRollsBackIdentity() {
        doThrow(new AuthenticationAccountConflictException(new IllegalStateException("simulated")))
                .when(accounts).create(org.mockito.ArgumentMatchers.any());
        assertThatThrownBy(() -> registration.createAdministratively(
                data("58392174097", null), UserActivationMethod.TEMPORARY_PASSWORD, 1L))
                .isInstanceOf(UserRegistrationConflictException.class);
        assertThat(users.existsByCpf("58392174097")).isFalse();
    }

    @Test
    void deliveryFailureCanBeRecoveredByReissue() {
        doThrow(new org.springframework.mail.MailSendException("simulated SMTP timeout",
                new java.net.SocketTimeoutException("read timed out")))
                .doNothing().when(mailSender).send(anyString(), anyString(), anyString());
        var result = registration.createAdministratively(
                data("12763845053", "delivery@example.com"), UserActivationMethod.EMAIL_INVITATION, 1L);
        assertThat(invitations.deliveryStatus(result.userId())).isEqualTo("DELIVERY_FAILED");
        assertThat(users.findById(result.userId())).isPresent();
        assertThat(accounts.existsByIdentityId(result.userId())).isTrue();
        String oldHash = invitationRepository.findById(result.userId()).orElseThrow().getTokenHash();
        invitations.issueForExistingUser(result.userId(), 1L);
        assertThat(invitations.deliveryStatus(result.userId())).isEqualTo("SENT");
        statusWriter.update(result.userId(), oldHash, "DELIVERY_FAILED");
        assertThat(invitations.deliveryStatus(result.userId())).isEqualTo("SENT");
    }

    @Test
    void smtpStatusUpdateDoesNotCompeteWithInvitationRotationVersion() {
        var result = registration.createAdministratively(data("35920478611", "version@example.com"),
                UserActivationMethod.EMAIL_INVITATION, 1L);
        var before = invitationRepository.findById(result.userId()).orElseThrow();
        statusWriter.update(result.userId(), before.getTokenHash(), "DELIVERY_FAILED");
        var after = invitationRepository.findById(result.userId()).orElseThrow();
        assertThat(after.getVersion()).isEqualTo(before.getVersion());
        assertThat(invitations.deliveryStatus(result.userId())).isEqualTo("DELIVERY_FAILED");
        invitations.issueForExistingUser(result.userId(), 1L);
        assertThat(invitations.deliveryStatus(result.userId())).isEqualTo("SENT");
    }

    @Test
    void persistentQueuedStatusRemainsVisibleAndCanBeRecovered() {
        var result = registration.createAdministratively(data("46310897544", "queued@example.com"),
                UserActivationMethod.EMAIL_INVITATION, 1L);
        var invitation = invitationRepository.findById(result.userId()).orElseThrow();
        invitation.setDeliveryStatus("QUEUED");
        invitationRepository.saveAndFlush(invitation);
        assertThat(registration.administrativeInvitationStatus(result.userId())).isEqualTo("QUEUED");
        invitations.issueForExistingUser(result.userId(), 1L);
        assertThat(registration.administrativeInvitationStatus(result.userId())).isEqualTo("SENT");
    }

    @Test
    void expiredOrBlockedInvitationCannotActivateAccount() {
        var result = registration.createAdministratively(data("51683094725", "expire@example.com"),
                UserActivationMethod.EMAIL_INVITATION, 1L);
        ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
        verify(mailSender).send(anyString(), anyString(), sent.capture());
        var invitation = invitationRepository.findById(result.userId()).orElseThrow();
        invitation.setExpiresAt(Instant.now(clock).minusSeconds(1));
        invitationRepository.saveAndFlush(invitation);
        assertThatThrownBy(() -> invitations.validate(sent.getValue()))
                .isInstanceOf(ActivationInvitationException.class);
        assertThatThrownBy(() -> invitations.complete(sent.getValue(), "Definitiva@123"))
                .isInstanceOf(ActivationInvitationException.class);

        invitations.issueForExistingUser(result.userId(), 1L);
        verify(mailSender, times(2)).send(anyString(), anyString(), sent.capture());
        String freshToken = sent.getAllValues().get(sent.getAllValues().size() - 1);
        var account = accounts.findByIdentityId(result.userId()).orElseThrow();
        for (int attempt = 0; attempt < 5; attempt++) account.registerFailedLogin();
        accounts.save(account);
        assertThatThrownBy(() -> invitations.validate(freshToken)).isInstanceOf(ActivationInvitationException.class);
        assertThatThrownBy(() -> invitations.complete(freshToken, "Definitiva@123"))
                .isInstanceOf(ActivationInvitationException.class);
        assertThat(accounts.findByIdentityId(result.userId()).orElseThrow().getCredentialStatus())
                .isEqualTo(CredentialStatus.PENDING_FIRST_ACCESS);
    }

    @Test
    void simultaneousCreationWithSameCpfProducesOneUserAndOneControlledConflict() throws Exception {
        var attempts = createConcurrently(data("82053416720", null), data("82053416720", null));
        assertOneCreationAndConflict(attempts);
        Long userId = users.findByCpf("82053416720").orElseThrow().id();
        assertThat(accounts.existsByIdentityId(userId)).isTrue();
    }

    @Test
    void simultaneousCreationWithSameEmailProducesOneUserAndOneControlledConflict() throws Exception {
        var attempts = createConcurrently(data("37648019504", "duplicate@example.com"),
                data("24590678143", "duplicate@example.com"));
        assertOneCreationAndConflict(attempts);
        Long userId = attempts.stream().filter(attempt -> attempt.userId() != null).findFirst().orElseThrow().userId();
        assertThat(accounts.existsByIdentityId(userId)).isTrue();
    }

    @Test
    void simultaneousReissuesLeaveOnlyNewestInvitationValid() throws Exception {
        var result = registration.createAdministratively(data("39057816426", "reissue@example.com"),
                UserActivationMethod.EMAIL_INVITATION, 1L);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> reissueAfter(start, result.userId()));
            var second = executor.submit(() -> reissueAfter(start, result.userId()));
            start.countDown();
            assertThat(first.get(15, TimeUnit.SECONDS)).isNull();
            assertThat(second.get(15, TimeUnit.SECONDS)).isNull();
        } finally {
            executor.shutdownNow();
        }
        ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
        verify(mailSender, times(3)).send(anyString(), anyString(), sent.capture());
        long valid = sent.getAllValues().stream().filter(this::isValidInvitation).count();
        assertThat(valid).isEqualTo(1);
        assertThat(invitationRepository.findById(result.userId())).isPresent();
        assertThat(accounts.findByIdentityId(result.userId()).orElseThrow().getCredentialStatus())
                .isEqualTo(CredentialStatus.PENDING_FIRST_ACCESS);
    }

    @Test
    void simultaneousActivationAndReissueCannotMakePermanentAccountPending() throws Exception {
        var result = registration.createAdministratively(data("68074512932", "race@example.com"),
                UserActivationMethod.EMAIL_INVITATION, 1L);
        ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
        verify(mailSender).send(anyString(), anyString(), sent.capture());
        String initialToken = sent.getValue();
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        Throwable completionFailure;
        Throwable reissueFailure;
        try {
            var completion = executor.submit(() -> completeAfter(start, initialToken));
            var reissue = executor.submit(() -> reissueAfter(start, result.userId()));
            start.countDown();
            completionFailure = completion.get(15, TimeUnit.SECONDS);
            reissueFailure = reissue.get(15, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
        assertThat((completionFailure == null ? 1 : 0) + (reissueFailure == null ? 1 : 0)).isEqualTo(1);
        assertThat(completionFailure == null ? reissueFailure : completionFailure)
                .isInstanceOf(ActivationInvitationException.class);
        var account = accounts.findByIdentityId(result.userId()).orElseThrow();
        if (completionFailure == null) {
            assertThat(account.getCredentialStatus()).isEqualTo(CredentialStatus.PERMANENT);
            assertThat(isValidInvitation(initialToken)).isFalse();
        } else {
            assertThat(account.getCredentialStatus()).isEqualTo(CredentialStatus.PENDING_FIRST_ACCESS);
            assertThat(isValidInvitation(initialToken)).isFalse();
            verify(mailSender, times(2)).send(anyString(), anyString(), sent.capture());
            assertThat(isValidInvitation(sent.getAllValues().get(sent.getAllValues().size() - 1))).isTrue();
        }
    }

    @Test
    void membershipOwnedPendingUserCannotReceiveAdministrativeInvitation() {
        String cpf = "90785634107";
        Long userId = registration.createPendingFirstAccess(data(cpf, "member@example.com"), "Provisoria@123");
        MembershipApplication application = membershipApplications.save(MembershipApplication.create(
                "Member", cpf, "member@example.com", "11999998888", null, Instant.now(clock)));
        application.approve(1L, userId, Instant.now(clock));
        membershipApplications.save(application);

        assertThatThrownBy(() -> invitations.issueForExistingUser(userId, 1L))
                .isInstanceOf(ActivationInvitationException.class);
        assertThat(invitationRepository.findById(userId)).isEmpty();
        assertThat(accounts.findByIdentityId(userId).orElseThrow().getCredentialStatus())
                .isEqualTo(CredentialStatus.PENDING_FIRST_ACCESS);
        assertThat(membershipApplications.findById(application.getId()).orElseThrow().getStatus())
                .isEqualTo(MembershipApplicationStatus.APPROVED);

        var challenge = (PasswordChangeRequiredLoginResult) sessions.login(cpf, "Provisoria@123");
        sessions.completeRequiredPasswordChange(challenge.passwordChangeToken(), "Definitiva@123");
        assertThat(membershipApplications.findById(application.getId()).orElseThrow().getStatus())
                .isEqualTo(MembershipApplicationStatus.COMPLETED);
    }

    @Test
    void administrativeInvitationCannotCompleteIfIdentityBecomesMembershipOwned() {
        String cpf = "91370682458";
        var result = registration.createAdministratively(data(cpf, "cross@example.com"),
                UserActivationMethod.EMAIL_INVITATION, 1L);
        ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
        verify(mailSender).send(anyString(), anyString(), sent.capture());
        MembershipApplication application = membershipApplications.save(MembershipApplication.create(
                "Cross", cpf, "cross@example.com", "11999998888", null, Instant.now(clock)));
        application.approve(1L, result.userId(), Instant.now(clock));
        membershipApplications.save(application);

        assertThatThrownBy(() -> invitations.complete(sent.getValue(), "Definitiva@123"))
                .isInstanceOf(ActivationInvitationException.class);
        assertThat(accounts.findByIdentityId(result.userId()).orElseThrow().getCredentialStatus())
                .isEqualTo(CredentialStatus.PENDING_FIRST_ACCESS);
        assertThat(membershipApplications.findById(application.getId()).orElseThrow().getStatus())
                .isEqualTo(MembershipApplicationStatus.APPROVED);
    }

    @Test
    void simultaneousActivationConsumesInvitationOnlyOnce() throws Exception {
        var result = registration.createAdministratively(
                data("65923017407", "concurrent@example.com"), UserActivationMethod.EMAIL_INVITATION, 1L);
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

    private Throwable reissueAfter(CountDownLatch start, Long userId) {
        try {
            start.await();
            invitations.issueForExistingUser(userId, 1L);
            return null;
        } catch (Throwable failure) {
            return failure;
        }
    }

    private boolean isValidInvitation(String token) {
        try {
            invitations.validate(token);
            return true;
        } catch (ActivationInvitationException ignored) {
            return false;
        }
    }

    private List<CreationAttempt> createConcurrently(UserRegistrationData firstData,
                                                       UserRegistrationData secondData) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> createAfter(start, firstData));
            var second = executor.submit(() -> createAfter(start, secondData));
            start.countDown();
            return List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private CreationAttempt createAfter(CountDownLatch start, UserRegistrationData data) {
        try {
            start.await();
            return new CreationAttempt(registration.createAdministratively(data,
                    UserActivationMethod.TEMPORARY_PASSWORD, 1L).userId(), null);
        } catch (Throwable failure) {
            return new CreationAttempt(null, failure);
        }
    }

    private void assertOneCreationAndConflict(List<CreationAttempt> attempts) {
        assertThat(attempts.stream().filter(attempt -> attempt.userId() != null).count()).isEqualTo(1);
        assertThat(attempts.stream().filter(attempt -> attempt.failure() instanceof UserRegistrationConflictException).count())
                .isEqualTo(1);
    }

    private record CreationAttempt(Long userId, Throwable failure) {}

    private UserRegistrationData data(String cpf, String email) {
        return new UserRegistrationData("Admin User", LocalDate.of(1990, 4, 12), email, cpf,
                cpf, "(11) 99999-8888", null, Instant.now(clock));
    }
}
