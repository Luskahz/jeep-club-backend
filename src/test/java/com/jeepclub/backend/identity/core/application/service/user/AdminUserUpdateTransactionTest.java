package com.jeepclub.backend.identity.core.application.service.user;

import com.jeepclub.backend.iam.identity.api.module.*;
import com.jeepclub.backend.iam.identity.api.http.dto.admin.user.AdminUpdateUserRequestDTO;
import com.jeepclub.backend.iam.identity.core.application.service.user.AdminUserService;
import com.jeepclub.backend.iam.identity.core.application.exception.AdministrativeUserUpdateConflictException;
import com.jeepclub.backend.iam.identity.core.repository.UserRepository;
import com.jeepclub.backend.iam.authentication.core.application.service.session.SessionService;
import com.jeepclub.backend.iam.authentication.core.application.result.login.AuthenticatedLoginResult;
import com.jeepclub.backend.iam.authentication.core.application.result.login.PasswordChangeRequiredLoginResult;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.login.PasswordChangeChallengeInvalidException;
import com.jeepclub.backend.iam.authentication.core.application.service.activation.AccountActivationInvitationService;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.account.ActivationInvitationException;
import com.jeepclub.backend.iam.authentication.infra.mail.AccountActivationMailSender;
import com.jeepclub.backend.iam.authentication.infra.mail.AccountActivationCurrentDelivery;
import com.jeepclub.backend.iam.authentication.core.application.service.activation.AccountActivationMailEvent;
import com.jeepclub.backend.iam.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.iam.authentication.core.domain.enums.CredentialStatus;
import com.jeepclub.backend.iam.identity.api.module.spi.UserAuthenticationAdministrationPort;
import com.jeepclub.backend.platform.logging.*;
import jakarta.validation.Validator;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class AdminUserUpdateTransactionTest {
    @Autowired UserRegistration registration;
    @Autowired UserQuery users;
    @Autowired AdminUserService admin;
    @Autowired SessionService sessions;
    @Autowired com.jeepclub.backend.iam.authentication.core.application.service.refreshtoken.RefreshTokenService refresh;
    @MockitoSpyBean Clock clock;
    @Autowired JsonMapper json;
    @Autowired Validator validator;
    @Autowired AuthenticationAccountRepository accounts;
    @Autowired AccountActivationInvitationService invitations;
    @Autowired AccountActivationCurrentDelivery delivery;
    @Autowired com.jeepclub.backend.iam.authentication.core.repository.PasswordRecoveryRequestRepository recoveries;
    @Autowired com.jeepclub.backend.iam.authentication.core.port.RefreshTokenHashService tokenHashes;
    @Autowired com.jeepclub.backend.iam.authentication.core.application.service.passwordrecovery.PasswordRecoveryService passwordRecovery;
    @Autowired com.jeepclub.backend.memberships.core.application.service.memberactivationtoken.MemberActivationTokenService memberActivations;
    @Autowired com.jeepclub.backend.memberships.core.port.MemberActivationTokenHashPort memberHashes;
    @MockitoSpyBean com.jeepclub.backend.memberships.core.port.CompletePendingFirstAccessPort firstAccess;
    @Autowired SystemLogJpaRepository logs;
    @Autowired com.jeepclub.backend.memberships.core.repository.MembershipApplicationRepository applications;
    @Autowired com.jeepclub.backend.memberships.core.repository.MemberActivationTokenRepository memberTokens;
    @MockitoSpyBean SystemLogService audit;
    @MockitoSpyBean UserRepository repository;
    @MockitoSpyBean UserAuthenticationAdministrationPort authentication;
    @MockitoBean AccountActivationMailSender mail;
    private static final AtomicInteger SEQUENCE = new AtomicInteger(2000);

    @AfterEach void cleanup() { reset(audit, repository, authentication, mail, clock, firstAccess); }
    UserRegistrationData data() {
        String cpf = cpf(SEQUENCE.incrementAndGet());
        return new UserRegistrationData("Original", null, "u" + cpf + "@example.com", cpf, null, null, null, clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
    }
    static String cpf(int value) {
        String digits = String.format("%09d", value);
        for (int count=9; count<=10; count++) {
            int sum=0; for (int i=0; i<count; i++) sum+=(digits.charAt(i)-'0')*(count+1-i);
            int check=11-sum%11; digits+=check>=10?0:check;
        }
        return digits;
    }
    void update(Long id, String body) throws Exception { admin.update(id, 999L, AdminUpdateUserRequestDTO.read(json.readTree(body), validator)); }

    @Test void auditFailureRollsBackContactAndSessionRevocation() throws Exception {
        var data = data();
        var tokens = registration.registerAndAuthenticate(data, "password-123");
        Long id = users.findByCpf(data.cpf()).orElseThrow().id();
        var before = users.findById(id).orElseThrow();
        long count = logs.count();
        doThrow(new IllegalStateException("audit unavailable")).when((SystemLogService) org.springframework.test.util.AopTestUtils.getUltimateTargetObject(audit)).recordInTransaction(any());
        assertThatThrownBy(() -> update(id, "{\"cpf\":\"" + cpf(SEQUENCE.incrementAndGet()) + "\",\"name\":\"New\"}"))
                .hasMessage("audit unavailable");
        assertThat(users.findById(id).orElseThrow()).isEqualTo(before);
        assertThat(logs.count()).isEqualTo(count);
        // The original refresh token remains valid because its revocation was rolled back.
        assertThat(refresh.rotate(tokens.refreshToken())).isNotNull();
        verify(authentication).identityChanged(eq(id), eq(true), eq(false), any());
    }

    @Test void authenticationFailureRollsBackIdentityBeforeAudit() throws Exception {
        var data = data(); Long id = registration.createWithPermanentCredential(data, "password-123");
        var before = users.findById(id).orElseThrow(); long count = logs.count();
        doThrow(new IllegalStateException("authentication unavailable")).when(authentication).identityChanged(anyLong(), anyBoolean(), anyBoolean(), any());
        assertThatThrownBy(() -> update(id, "{\"email\":\"changed@example.com\"}")).hasMessage("authentication unavailable");
        assertThat(users.findById(id).orElseThrow()).isEqualTo(before);
        assertThat(logs.count()).isEqualTo(count);
    }

    @Test void profileAuditFailureRollsBackFirstUpsert() {
        Long id = registration.createWithPermanentCredential(data(), "password-123");
        doThrow(new IllegalStateException("audit unavailable")).when((SystemLogService) org.springframework.test.util.AopTestUtils.getUltimateTargetObject(audit)).recordInTransaction(any());
        assertThatThrownBy(() -> admin.replaceProfile(id, 999L,
                new com.jeepclub.backend.iam.identity.core.domain.model.WorkProfile("Driver", "Club"), null)).hasMessage("audit unavailable");
        assertThat(admin.getProfile(id).workProfile()).isNull();
    }

    @Test void cpfChangeRevokesOldSessionAndChallengeWithoutChangingPasswordState() throws Exception {
        var data = data();
        var created = registration.createAdministratively(data, UserActivationMethod.TEMPORARY_PASSWORD, 999L);
        var challenge = (PasswordChangeRequiredLoginResult) sessions.login(data.cpf(), created.temporaryPassword());
        String newCpf = cpf(SEQUENCE.incrementAndGet());
        update(created.userId(), "{\"cpf\":\"" + newCpf + "\"}");
        assertThatThrownBy(() -> sessions.completeRequiredPasswordChange(challenge.passwordChangeToken(), "new-password"))
                .isInstanceOf(PasswordChangeChallengeInvalidException.class);
        assertThat(accounts.findByIdentityId(created.userId()).orElseThrow().getCredentialStatus()).isEqualTo(CredentialStatus.PENDING_FIRST_ACCESS);
        assertThat(sessions.login(newCpf, created.temporaryPassword())).isInstanceOf(PasswordChangeRequiredLoginResult.class);
    }

    @Test void emailChangeInvalidatesOldTokenAndReissueUsesNewAddress() throws Exception {
        var data = data();
        var created = registration.createAdministratively(data, UserActivationMethod.EMAIL_INVITATION, 999L);
        ArgumentCaptor<String> raw = ArgumentCaptor.forClass(String.class);
        verify(mail).send(eq(data.email()), anyString(), raw.capture());
        String oldToken = raw.getValue();
        update(created.userId(), "{\"email\":\"new-address@example.com\"}");
        assertThatThrownBy(() -> invitations.validate(oldToken)).isInstanceOf(ActivationInvitationException.class);
        assertThatThrownBy(() -> invitations.complete(oldToken, "new-password")).isInstanceOf(ActivationInvitationException.class);
        assertThat(invitations.deliveryStatus(created.userId())).isEqualTo("INVALIDATED");
        invitations.issueForExistingUser(created.userId(), 999L);
        verify(mail).send(eq("new-address@example.com"), anyString(), raw.capture());
        invitations.complete(raw.getValue(), "new-password");
        assertThat(accounts.findByIdentityId(created.userId()).orElseThrow().getCredentialStatus()).isEqualTo(CredentialStatus.PERMANENT);
    }

    @Test void staleQueuedMailIsSkippedAfterContactCorrection() throws Exception {
        var data = data();
        var created = registration.createAdministratively(data, UserActivationMethod.EMAIL_INVITATION, 999L);
        ArgumentCaptor<String> raw = ArgumentCaptor.forClass(String.class);
        verify(mail).send(eq(data.email()), anyString(), raw.capture());
        update(created.userId(), "{\"email\":null}");
        clearInvocations(mail);
        assertThat(delivery.sendIfCurrent(new AccountActivationMailEvent(created.userId(), data.email(), data.name(), raw.getValue(), "obsolete-hash"))).isFalse();
        verifyNoInteractions(mail);
    }

    @Test void concurrentPatchesPreserveFieldsOmittedByTheOtherRequest() throws Exception {
        Long id = registration.createWithPermanentCredential(data(), "password-123");
        ExecutorService pool = Executors.newFixedThreadPool(2); CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> first = pool.submit(() -> { start.await(); update(id, "{\"name\":\"New\"}"); return null; });
            Future<?> second = pool.submit(() -> { start.await(); update(id, "{\"phoneNumber\":\"11988887777\"}"); return null; });
            start.countDown(); first.get(20, TimeUnit.SECONDS); second.get(20, TimeUnit.SECONDS);
            var result=users.findById(id).orElseThrow(); assertThat(result.name()).isEqualTo("New"); assertThat(result.phoneNumber()).isEqualTo("11988887777");
        } finally { pool.shutdownNow(); }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"cpf", "email", "rg"})
    void concurrentDuplicatesAre409AndLosingTransactionKeepsAllOriginalFields(String field) throws Exception {
        Long first = registration.createWithPermanentCredential(data(), "password-123");
        Long second = registration.createWithPermanentCredential(data(), "password-123");
        var oldFirst=users.findById(first).orElseThrow(); var oldSecond=users.findById(second).orElseThrow();
        String value = field.equals("email") ? "race" + SEQUENCE.incrementAndGet() + "@example.com" : cpf(SEQUENCE.incrementAndGet());
        CyclicBarrier checks = new CyclicBarrier(2);
        org.mockito.stubbing.Answer<Boolean> synchronize = call -> {
            boolean exists=(boolean)call.callRealMethod();
            if(value.equals(call.getArgument(0))) checks.await(10, TimeUnit.SECONDS);
            return exists;
        };
        switch(field) {
            case "cpf" -> doAnswer(synchronize).when(repository).existsByCpfAndIdNot(anyString(),anyLong());
            case "email" -> doAnswer(synchronize).when(repository).existsByEmailAndIdNot(anyString(),anyLong());
            case "rg" -> doAnswer(synchronize).when(repository).existsByRgAndIdNot(anyString(),anyLong());
            default -> throw new AssertionError(field);
        }
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            java.util.List<Future<Boolean>> results=java.util.List.of(first,second).stream().map(id -> pool.submit(() -> {
                try { update(id,"{\""+field+"\":\""+value+"\",\"name\":\"Changed\"}"); return true; }
                catch(AdministrativeUserUpdateConflictException expected) { return false; }
            })).toList();
            assertThat(java.util.List.of(results.get(0).get(20,TimeUnit.SECONDS),results.get(1).get(20,TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true,false);
            var a=users.findById(first).orElseThrow(); var b=users.findById(second).orElseThrow();
            boolean firstWon = "Changed".equals(a.name());
            assertThat(firstWon ? b : a).isEqualTo(firstWon ? oldSecond : oldFirst);
        } finally { pool.shutdownNow(); }
    }
    @Test void loginWithOldCpfWaitsForAdministrativeCorrectionAndThenFails() throws Exception {
        var data=data(); Long id=registration.createWithPermanentCredential(data,"password-123");
        CountDownLatch locked=new CountDownLatch(1), release=new CountDownLatch(1), loginAttempted=new CountDownLatch(1);
        doAnswer(call -> { Object result=call.callRealMethod(); if(id.equals(call.getArgument(0))) { locked.countDown(); release.await(10,TimeUnit.SECONDS); } return result; })
                .when(repository).findByIdForUpdate(anyLong());
        doAnswer(call -> { loginAttempted.countDown(); return call.callRealMethod(); }).when(repository).findByCpfForUpdate(eq(data.cpf()));
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            Future<?> edit=pool.submit(() -> { update(id,"{\"cpf\":\""+cpf(SEQUENCE.incrementAndGet())+"\"}"); return null; });
            assertThat(locked.await(10,TimeUnit.SECONDS)).isTrue();
            Future<?> login=pool.submit(() -> sessions.login(data.cpf(),"password-123"));
            assertThat(loginAttempted.await(10,TimeUnit.SECONDS)).isTrue();
            release.countDown(); edit.get(20,TimeUnit.SECONDS);
            assertThatThrownBy(() -> login.get(20,TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(com.jeepclub.backend.iam.authentication.core.application.exceptions.login.InvalidCredentialsException.class);
        } finally { release.countDown(); pool.shutdownNow(); }
    }

    @Test void cpfCorrectionRevokesExistingRefreshWithoutChangingAccountState() throws Exception {
        var data=data(); var tokens=registration.registerAndAuthenticate(data,"password-123");
        Long id=users.findByCpf(data.cpf()).orElseThrow().id();
        var before=accounts.findByIdentityId(id).orElseThrow();
        update(id,"{\"cpf\":\""+cpf(SEQUENCE.incrementAndGet())+"\"}");
        assertThatThrownBy(() -> refresh.rotate(tokens.refreshToken()))
                .isInstanceOf(com.jeepclub.backend.iam.authentication.core.application.exceptions.refreshtoken.RefreshTokenInvalidException.class);
        var after=accounts.findByIdentityId(id).orElseThrow();
        assertThat(after.getPasswordHash()).isEqualTo(before.getPasswordHash());
        assertThat(after.getCredentialStatus()).isEqualTo(before.getCredentialStatus());
        assertThat(after.getAccessStatus()).isEqualTo(before.getAccessStatus());
        assertThat(after.getFailedLoginAttempts()).isEqualTo(before.getFailedLoginAttempts());
    }

    @Test void emailCorrectionInvalidatesMembershipInvitationWithoutRewritingHistoricalApplication() throws Exception {
        var data=data(); Long id=registration.createPendingFirstAccess(data,"temporary-password");
        var application=com.jeepclub.backend.memberships.core.domain.model.MembershipApplication.create(
                data.name(),data.cpf(),data.email(),"11999999999",null,data.now());
        application.approve(999L,id,clock.instant());
        var saved=applications.save(application);
        var original=applications.findById(saved.getId()).orElseThrow();
        memberTokens.save(com.jeepclub.backend.memberships.core.domain.model.MemberActivationToken.create(
                saved.getId(), "hash"+saved.getId(),java.time.Duration.ofHours(24),clock.instant()));
        update(id,"{\"email\":\"member-corrected@example.com\"}");
        assertThat(memberTokens.findLatestByApplicationId(saved.getId()).orElseThrow().getUsedAt()).isNotNull();
        var after=applications.findById(saved.getId()).orElseThrow();
        assertThat(after.getEmail()).isEqualTo(original.getEmail());
        assertThat(after.getCpf()).isEqualTo(original.getCpf());
        assertThat(after.getCreatedUserId()).isEqualTo(id);
        assertThat(after.getStatus()).isEqualTo(original.getStatus());
    }

    @Test void emailCorrectionMustCancelEveryOpenEmailRecoveryToken() throws Exception {
        var data=data(); Long id=registration.createWithPermanentCredential(data,"password-123");
        var now=clock.instant();
        var first=com.jeepclub.backend.iam.authentication.core.domain.model.PasswordRecoveryRequest.createOpenRequest(id,now.minusSeconds(10),now.plusSeconds(3600));
        first.changeToEmailTokenMethod(tokenHashes.hash("first-old-token"),now);
        recoveries.save(first);
        var second=com.jeepclub.backend.iam.authentication.core.domain.model.PasswordRecoveryRequest.createOpenRequest(id,now.minusSeconds(5),now.plusSeconds(3600));
        second.changeToEmailTokenMethod(tokenHashes.hash("second-old-token"),now);
        recoveries.save(second);
        update(id,"{\"email\":\"all-links-corrected@example.com\"}");
        assertThat(recoveries.findByUserId(id)).hasSize(2).allSatisfy(request -> {
            assertThat(request.getStatus()).isEqualTo(com.jeepclub.backend.iam.authentication.core.domain.enums.PasswordRecoveryRequestStatus.CANCELLED);
            assertThat(request.getCancelledAt()).isNotNull();
        });
        for(String oldToken:java.util.List.of("first-old-token","second-old-token")) {
            assertThatThrownBy(() -> passwordRecovery.resetPasswordByToken(oldToken,"new-password"))
                    .isInstanceOf(com.jeepclub.backend.iam.authentication.core.application.exceptions.tokenhash.TokenInvalidException.class);
        }
    }

    @Test void membershipExpiryAfterAuthenticationRollsBackCredentialChange() {
        var now=clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        java.util.concurrent.atomic.AtomicReference<Instant> current=new java.util.concurrent.atomic.AtomicReference<>(now);
        doAnswer(call -> current.get()).when(clock).instant();
        var data=data(); Long id=registration.createPendingFirstAccess(data,"temporary-password");
        var before=accounts.findByIdentityId(id).orElseThrow();
        var application=com.jeepclub.backend.memberships.core.domain.model.MembershipApplication.create(data.name(),data.cpf(),data.email(),"11999999999",null,now);
        application.approve(999L,id,now);
        var saved=applications.save(application);
        String raw="member-expiring-token";
        memberTokens.save(com.jeepclub.backend.memberships.core.domain.model.MemberActivationToken.create(saved.getId(),memberHashes.hash(raw),java.time.Duration.ofSeconds(60),now));
        doAnswer(call -> {
            call.callRealMethod();
            current.set(now.plusSeconds(120));
            return null;
        }).when(firstAccess).complete(eq(id),eq("new-password"));
        assertThatThrownBy(() -> memberActivations.complete(raw,"new-password"))
                .isInstanceOf(com.jeepclub.backend.memberships.core.application.exception.MemberActivationTokenExpiredException.class);
        var after=accounts.findByIdentityId(id).orElseThrow();
        assertThat(after.getPasswordHash()).isEqualTo(before.getPasswordHash());
        assertThat(after.getCredentialStatus()).isEqualTo(CredentialStatus.PENDING_FIRST_ACCESS);
        assertThat(memberTokens.findLatestByApplicationId(saved.getId()).orElseThrow().getUsedAt()).isNull();
        assertThat(applications.findById(saved.getId()).orElseThrow().getStatus())
                .isEqualTo(com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus.APPROVED);
    }

    @Test void missingAuthenticationAccountRollsBackTheActualServiceTransaction() throws Exception {
        var data=data();
        var orphan=repository.create(com.jeepclub.backend.iam.identity.core.domain.model.User.create(data.name(),data.birthDate(),data.email(),data.cpf(),data.rg(),data.phoneNumber(),null,data.now()));
        var before=users.findById(orphan.getId()).orElseThrow();
        assertThatThrownBy(() -> update(orphan.getId(),"{\"email\":\"orphan-corrected@example.com\"}"))
                .isInstanceOf(com.jeepclub.backend.iam.identity.api.module.exception.UserIdentityChangeConflictException.class);
        assertThat(users.findById(orphan.getId()).orElseThrow()).isEqualTo(before);
    }

}
