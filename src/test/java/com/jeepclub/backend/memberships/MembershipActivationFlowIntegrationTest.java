package com.jeepclub.backend.memberships;

import com.jayway.jsonpath.JsonPath;
import com.jeepclub.backend.iam.authentication.core.domain.enums.CredentialStatus;
import com.jeepclub.backend.iam.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.iam.authentication.infra.persistence.jpa.AuthenticationAccountJpaRepository;
import com.jeepclub.backend.iam.authorization.core.domain.model.*;
import com.jeepclub.backend.iam.authorization.core.repository.*;
import com.jeepclub.backend.iam.identity.api.module.*;
import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.memberships.core.port.MemberActivationMailSender;
import com.jeepclub.backend.memberships.core.port.MemberActivationTokenHashPort;
import com.jeepclub.backend.memberships.core.repository.MembershipApplicationRepository;
import com.jeepclub.backend.memberships.infra.persistence.jpa.MemberActivationTokenJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Committed HTTP transactions with real Identity/Authentication and H2 persistence. */
@SpringBootTest(properties={"memberships.registry.issuance.enabled=true","memberships.registry.issuance.approved=true","memberships.registry.issuance.initial-number=1000","memberships.registry.issuance.width=4","memberships.registry.issuance.prefix="})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MembershipActivationFlowIntegrationTest {
    private static final String PASSWORD = "DefinitivePassword2026!";
    @Autowired MockMvc mvc;
    @Autowired UserRegistration registration;
    @Autowired UserQuery identities;
    @Autowired AuthenticationAccountJpaRepository accounts;
    @Autowired MemberActivationTokenJpaRepository tokens;
    @Autowired MemberActivationTokenHashPort hashes;
    @Autowired PasswordHasher passwords;
    @Autowired RoleRepository roles;
    @Autowired UserRoleRepository userRoles;
    @Autowired PermissionRepository permissions;
    @Autowired RolePermissionRepository rolePermissions;
    @Autowired Clock clock;
    @Autowired com.jeepclub.backend.memberships.api.module.MemberRegistrationQuery registryQuery;
    @Autowired com.jeepclub.backend.memberships.infra.persistence.jpa.MemberRegistrySequenceJpaRepository registrySequence;
    @Autowired com.jeepclub.backend.memberships.infra.config.MemberIssuanceProperties issuance;
    @MockitoSpyBean com.jeepclub.backend.memberships.infra.persistence.jpa.MemberRegistryAuditJpaRepository registryAudits;
    @MockitoSpyBean MembershipApplicationRepository applications;
    @MockitoBean MemberActivationMailSender mail;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Autowired com.jeepclub.backend.memberships.core.application.service.membershipapplication.AdminMembershipApplicationService administration;
    @Autowired com.jeepclub.backend.memberships.core.application.service.memberactivationtoken.MemberActivationTokenService activation;
    @Autowired UserIdentityLock identityLock;
    @Autowired com.jeepclub.backend.iam.authentication.core.repository.AccountActivationInvitationRepository invitations;
    @Autowired com.jeepclub.backend.iam.authentication.core.application.service.session.SessionService authentication;
    @Autowired com.jeepclub.backend.billing.core.application.service.chargedefinition.AdminChargeDefinitionService definitions;
    @Autowired com.jeepclub.backend.billing.core.application.service.chargeassignment.AdminChargeAssignmentService assignments;
    @MockitoSpyBean com.jeepclub.backend.memberships.infra.persistence.jpa.MembershipApplicationReviewJpaRepository reviews;
    @MockitoSpyBean com.jeepclub.backend.memberships.infra.persistence.jpa.MembershipApplicantBlockJpaRepository blocks;
    private static final java.util.concurrent.atomic.AtomicInteger CANDIDATES = new java.util.concurrent.atomic.AtomicInteger(810000000);
    private String admin;
    private Long adminId;

    @BeforeEach
    void administratorWithRealCredentialsAndApprovalPermission() throws Exception {
        if(!registrySequence.existsById(1L)) {
            var row=new com.jeepclub.backend.memberships.infra.persistence.entity.MemberRegistrySequenceEntity();row.setId(1L);registrySequence.saveAndFlush(row);
        }
        String cpf = "73045198279";
        if (!identities.existsByCpf(cpf)) {
            Long id = registration.createWithPermanentCredential(new UserRegistrationData(
                    "Onboarding Admin", null, null, cpf, null, null, null, Instant.now(clock)), PASSWORD);
            Role role = roles.save(Role.create("onboarding-audit", "Audit", Instant.now(clock)));
            userRoles.save(UserRole.create(id, role.getId(), Instant.now(clock)));
            for (var code : java.util.List.of(
                    com.jeepclub.backend.shared.authorization.PermissionCode.MEMBERSHIP_MEMBERSHIP_REQUEST_APPROVE,
                    com.jeepclub.backend.shared.authorization.PermissionCode.MEMBERSHIP_MEMBERSHIP_REQUEST_READ,
                    com.jeepclub.backend.shared.authorization.PermissionCode.MEMBERSHIP_MEMBERSHIP_REQUEST_REJECT,
                    com.jeepclub.backend.shared.authorization.PermissionCode.MEMBERSHIP_MEMBERSHIP_APPLICANT_BLOCK)) {
                var permission = permissions.findByCode(code).orElseThrow();
                rolePermissions.save(RolePermission.create(role.getId(), permission.getId(), Instant.now(clock)));
            }
        }
        adminId = identities.findByCpf(cpf).orElseThrow().id();
        admin = "Bearer " + json(login(cpf, PASSWORD).andExpect(status().isOk()), "$.accessToken");
    }

    @Test
    void completeDeterministicOnboardingFlow() throws Exception {
        Long id = submit("52998224725");
        mvc.perform(post("/membership-applications").contentType(MediaType.APPLICATION_JSON)
                        .content(submission("52998224725")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.created").value(false)).andExpect(jsonPath("$.email").doesNotExist());
        approve(id).andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
        var approved = applications.findById(id).orElseThrow();
        Long userId = approved.getCreatedUserId();
        assertThat(registryQuery.byIdentityUserId(userId)).isEmpty();
        assertThat(approved.getStatus()).isEqualTo(MembershipApplicationStatus.APPROVED);
        assertThat(identities.findById(userId).orElseThrow().cpf()).isEqualTo("52998224725");
        assertThat(accounts.findById(userId).orElseThrow().getCredentialStatus())
                .isEqualTo(CredentialStatus.PENDING_FIRST_ACCESS);
        String raw = deliveredToken(1);
        var persistedToken = tokens.findByTokenHash(hashes.hash(raw)).orElseThrow();
        assertThat(persistedToken.getApplicationId()).isEqualTo(id);
        assertThat(persistedToken.getTokenHash()).isNotEqualTo(raw);
        mvc.perform(get("/membership-applications/activate").param("token", raw))
                .andExpect(status().isNoContent());
        assertThat(tokens.findById(persistedToken.getId()).orElseThrow().getUsedAt()).isNull();
        assertThat(applications.findById(id).orElseThrow().getFinishedAt()).isNull();
        complete(raw).andExpect(status().isNoContent());
        assertThat(applications.findById(id).orElseThrow().getStatus()).isEqualTo(MembershipApplicationStatus.COMPLETED);
        assertThat(applications.findById(id).orElseThrow().getFinishedAt()).isNotNull();
        assertThat(registryQuery.byIdentityUserId(applications.findById(id).orElseThrow().getCreatedUserId())).isPresent();
        var account = accounts.findById(userId).orElseThrow();
        assertThat(account.getCredentialStatus()).isEqualTo(CredentialStatus.PERMANENT);
        assertThat(registryQuery.byIdentityUserId(userId)).isPresent();
        assertThat(passwords.matches(PASSWORD, account.getPasswordHash())).isTrue();
        assertThat(tokens.findById(persistedToken.getId()).orElseThrow().getUsedAt()).isNotNull();
        String access = json(login("52998224725", PASSWORD).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHENTICATED")), "$.accessToken");
        mvc.perform(get("/authentication/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.sessionActive").value(true));
        complete(raw).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEMBERSHIP_FIRST_ACCESS_CONFLICT"));
        mvc.perform(get("/membership-applications/activate").param("token", raw))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ACTIVATION_TOKEN_ALREADY_USED"));
        approve(id).andExpect(status().isConflict());
    }

    @Test
    void resendInvalidatesPreviousPersistedTokenAndLeavesFirstAccessPending() throws Exception {
        var registryBefore=registrySnapshot();
        Long id = submit("16899535009");
        approve(id).andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
        String old = deliveredToken(1);
        mvc.perform(post("/admin/membership-applications/{id}/activation-link/resend", id)
                        .header("Authorization", admin)).andExpect(status().isNoContent());
        String replacement = deliveredToken(2);
        assertThat(replacement).isNotEqualTo(old);
        complete(old).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ACTIVATION_TOKEN_ALREADY_USED"));
        mvc.perform(get("/membership-applications/activate").param("token", replacement))
                .andExpect(status().isNoContent());
        assertThat(applications.findById(id).orElseThrow().getStatus()).isEqualTo(MembershipApplicationStatus.APPROVED);
        assertThat(accounts.findById(applications.findById(id).orElseThrow().getCreatedUserId()).orElseThrow()
                .getCredentialStatus()).isEqualTo(CredentialStatus.PENDING_FIRST_ACCESS);
        assertThat(registrySnapshot()).isEqualTo(registryBefore);
    }

    @Test
    void expiredAndUnknownTokensDoNotChangeCredentialsOrMembership() throws Exception {
        var registryBefore=registrySnapshot();
        Long id = submit("11144477735");
        approve(id).andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
        String raw = deliveredToken(1);
        var token = tokens.findByTokenHash(hashes.hash(raw)).orElseThrow();
        // Prove an expired token without depending on wall-clock advancement
        // between two fast requests or on database fractional-second rounding.
        token.setExpiresAt(Instant.now(clock).minusSeconds(1));
        tokens.saveAndFlush(token);
        mvc.perform(get("/membership-applications/activate").param("token", raw))
                .andExpect(status().isGone()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("ACTIVATION_TOKEN_EXPIRED"));
        complete(raw).andExpect(status().isGone()).andExpect(jsonPath("$.status").value(410));
        complete("unknown-token").andExpect(status().isNotFound());
        assertThat(tokens.findById(token.getId()).orElseThrow().getUsedAt()).isNull();
        assertThat(applications.findById(id).orElseThrow().getStatus()).isEqualTo(MembershipApplicationStatus.APPROVED);
        assertThat(accounts.findById(applications.findById(id).orElseThrow().getCreatedUserId()).orElseThrow()
                .getCredentialStatus()).isEqualTo(CredentialStatus.PENDING_FIRST_ACCESS);
        assertThat(registrySnapshot()).isEqualTo(registryBefore);
    }

    @Test
    void deliveryFailureRollsBackApprovalIdentityAccountAndTokenThenAllowsRetry() throws Exception {
        Long id = submit("12345678909");
        long accountCount = accounts.count();
        long tokenCount = tokens.count();
        doThrow(new RuntimeException("injected delivery failure")).when(mail).sendActivationLink(any(), any(), any());
        approve(id).andExpect(status().isInternalServerError());
        assertThat(applications.findById(id).orElseThrow().getStatus()).isEqualTo(MembershipApplicationStatus.PENDING);
        assertThat(applications.findById(id).orElseThrow().getCreatedUserId()).isNull();
        assertThat(identities.findByCpf("12345678909")).isEmpty();
        assertThat(accounts.count()).isEqualTo(accountCount);
        assertThat(tokens.count()).isEqualTo(tokenCount);
        reset(mail);
        approve(id).andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
        assertThat(identities.findByCpf("12345678909")).isPresent();
        assertThat(deliveredToken(1)).isNotBlank();
    }

    @Test
    void finalMembershipWriteFailureRollsBackRealCredentialChangeAndTokenConsumption() throws Exception {
        Long id = submit("98765432100");
        approve(id).andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
        String raw = deliveredToken(1);
        Long userId = applications.findById(id).orElseThrow().getCreatedUserId();
        String oldHash = accounts.findById(userId).orElseThrow().getPasswordHash();
        // Fault injection occurs after the real Authentication adapter and token save.
        doThrow(new RuntimeException("injected final write failure")).when(applications)
                .save(argThat(app -> app != null && app.getStatus() == MembershipApplicationStatus.COMPLETED));
        try {
            complete(raw).andExpect(status().isInternalServerError());
        } finally { reset(applications); }
        assertThat(accounts.findById(userId).orElseThrow().getPasswordHash()).isEqualTo(oldHash);
        assertThat(accounts.findById(userId).orElseThrow().getCredentialStatus()).isEqualTo(CredentialStatus.PENDING_FIRST_ACCESS);
        assertThat(tokens.findByTokenHash(hashes.hash(raw)).orElseThrow().getUsedAt()).isNull();
        assertThat(applications.findById(id).orElseThrow().getStatus()).isEqualTo(MembershipApplicationStatus.APPROVED);
        complete(raw).andExpect(status().isNoContent());
        assertThat(accounts.findById(userId).orElseThrow().getCredentialStatus()).isEqualTo(CredentialStatus.PERMANENT);
        assertThat(registryQuery.byIdentityUserId(userId)).isPresent();
    }

    @Test
    void temporaryPasswordFirstAccessAlsoCompletesMembershipThroughRealBoundary() throws Exception {
        Long id = submit("39053344705");
        String temporary = json(mvc.perform(post("/admin/membership-applications/{id}/approve/temporary-password", id)
                .header("Authorization", admin)).andExpect(status().isOk()), "$.temporaryPassword");
        String challenge = json(login("39053344705", temporary).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PASSWORD_CHANGE_REQUIRED")), "$.passwordChangeToken");
        mvc.perform(post("/authentication/login/password-change").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passwordChangeToken\":\"" + challenge + "\",\"newPassword\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk());
        assertThat(applications.findById(id).orElseThrow().getStatus()).isEqualTo(MembershipApplicationStatus.COMPLETED);
        assertThat(applications.findById(id).orElseThrow().getFinishedAt()).isNotNull();
        assertThat(registryQuery.byIdentityUserId(applications.findById(id).orElseThrow().getCreatedUserId())).isPresent();
    }

    @Test
    void temporaryPasswordRegistryFailureRollsBackCredentialsSessionChallengeAndSequenceThenRetries() throws Exception {
        String cpf=nextCpf();Long id=submit(cpf);
        String temporary=json(mvc.perform(post("/admin/membership-applications/{id}/approve/temporary-password",id)
                .header("Authorization",admin)).andExpect(status().isOk()),"$.temporaryPassword");
        Long user=applications.findById(id).orElseThrow().getCreatedUserId();
        String challenge=json(login(cpf,temporary).andExpect(status().isOk()),"$.passwordChangeToken");
        String oldHash=accounts.findById(user).orElseThrow().getPasswordHash();
        var before=registrySnapshot();
        long sessions=jdbc.queryForObject("select count(*) from authentication_sessions where user_id=?",Long.class,user);
        doThrow(new IllegalStateException("synthetic temporary-password registry failure")).when(registryAudits).saveAndFlush(any());
        try { changePassword(challenge).andExpect(status().isInternalServerError()); }
        finally { reset(registryAudits); }
        assertThat(registrySnapshot()).isEqualTo(before);assertThat(registryQuery.byIdentityUserId(user)).isEmpty();
        assertThat(accounts.findById(user).orElseThrow().getPasswordHash()).isEqualTo(oldHash);
        assertThat(accounts.findById(user).orElseThrow().getCredentialStatus()).isEqualTo(CredentialStatus.PENDING_FIRST_ACCESS);
        assertThat(applications.findById(id).orElseThrow().getStatus()).isEqualTo(MembershipApplicationStatus.APPROVED);
        assertThat(jdbc.queryForObject("select count(*) from authentication_sessions where user_id=?",Long.class,user)).isEqualTo(sessions);
        changePassword(challenge).andExpect(status().isOk());
        var assigned=registryQuery.byIdentityUserId(user).orElseThrow();var after=registrySnapshot();
        changePassword(challenge).andExpect(status().isUnauthorized());
        assertThat(registryQuery.byIdentityUserId(user)).contains(assigned);assertThat(registrySnapshot()).isEqualTo(after);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    void disabledIssuanceAllowsBothFirstAccessMethodsAndLegacyLoginWithoutRegistryWrites(boolean temporary) throws Exception {
        boolean wasEnabled=issuance.enabled();issuance.setEnabled(false);
        try {
            var before=registrySnapshot();String cpf=nextCpf();Long id=submit(cpf);
            if(temporary) {
                String password=json(mvc.perform(post("/admin/membership-applications/{id}/approve/temporary-password",id)
                        .header("Authorization",admin)).andExpect(status().isOk()),"$.temporaryPassword");
                String challenge=json(login(cpf,password).andExpect(status().isOk()),"$.passwordChangeToken");
                changePassword(challenge).andExpect(status().isOk());
            } else {
                approve(id).andExpect(status().isOk());complete(deliveredToken(1)).andExpect(status().isNoContent());
            }
            Long user=applications.findById(id).orElseThrow().getCreatedUserId();
            assertThat(applications.findById(id).orElseThrow().getStatus()).isEqualTo(MembershipApplicationStatus.COMPLETED);
            assertThat(registryQuery.byIdentityUserId(user)).isEmpty();assertThat(registryQuery.byIdentityUserId(adminId)).isEmpty();
            String access=json(login(cpf,PASSWORD).andExpect(status().isOk()),"$.accessToken");
            mvc.perform(get("/authentication/me").header("Authorization","Bearer "+access)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(user));
            assertThat(registrySnapshot()).isEqualTo(before);
        } finally { issuance.setEnabled(wasEnabled); }
    }

    @Test
    void registryAuditFailureRollsBackActivationCredentialTokenAndApplication() throws Exception {
        Long id=submit("98500199997");approve(id).andExpect(status().isOk());String raw=deliveredToken(1);
        Long userId=applications.findById(id).orElseThrow().getCreatedUserId();
        String hash=accounts.findById(userId).orElseThrow().getPasswordHash();
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("synthetic registry audit failure")).when(registryAudits).saveAndFlush(any());
        try { complete(raw).andExpect(status().isInternalServerError()); }
        finally { reset(registryAudits); }
        assertThat(registryQuery.byIdentityUserId(userId)).isEmpty();
        assertThat(accounts.findById(userId).orElseThrow().getPasswordHash()).isEqualTo(hash);
        assertThat(applications.findById(id).orElseThrow().getStatus()).isEqualTo(MembershipApplicationStatus.APPROVED);
        assertThat(tokens.findByTokenHash(hashes.hash(raw)).orElseThrow().getUsedAt()).isNull();
        complete(raw).andExpect(status().isNoContent());assertThat(registryQuery.byIdentityUserId(userId)).isPresent();
    }

    @Test
    void concurrentApprovalsCreateOneIdentityAndDeferMemberUntilFirstAccess() throws Exception {
        String cpf="98500200162";Long id=submit(cpf);long before=accounts.count();
        var ready=new java.util.concurrent.CountDownLatch(2);var go=new java.util.concurrent.CountDownLatch(1);
        var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
        org.springframework.test.web.servlet.MvcResult winner;
        try {
            java.util.concurrent.Callable<org.springframework.test.web.servlet.MvcResult> approve=() -> {
                ready.countDown();go.await();
                return mvc.perform(post("/admin/membership-applications/{id}/approve/temporary-password",id).header("Authorization",admin)).andReturn();
            };
            var first=executor.submit(approve);var second=executor.submit(approve);
            assertThat(ready.await(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue();go.countDown();
            var a=first.get(30,java.util.concurrent.TimeUnit.SECONDS);var b=second.get(30,java.util.concurrent.TimeUnit.SECONDS);
            assertThat(java.util.List.of(a.getResponse().getStatus(),b.getResponse().getStatus())).containsExactlyInAnyOrder(200,409);
            winner=a.getResponse().getStatus()==200?a:b;
        } finally {go.countDown();executor.shutdownNow();}
        Long userId=applications.findById(id).orElseThrow().getCreatedUserId();
        assertThat(accounts.count()).isEqualTo(before+1);assertThat(identities.findByCpf(cpf).orElseThrow().id()).isEqualTo(userId);
        assertThat(registryQuery.byIdentityUserId(userId)).isEmpty();
        String temporary=JsonPath.read(winner.getResponse().getContentAsString(),"$.temporaryPassword");
        String challenge=json(login(cpf,temporary).andExpect(status().isOk()),"$.passwordChangeToken");
        mvc.perform(post("/authentication/login/password-change").contentType(MediaType.APPLICATION_JSON)
                .content("{\"passwordChangeToken\":\""+challenge+"\",\"newPassword\":\""+PASSWORD+"\"}"))
                .andExpect(status().isOk());
        assertThat(registryQuery.byIdentityUserId(userId)).isPresent();
        mvc.perform(post("/admin/membership-applications/{id}/approve/temporary-password",id).header("Authorization",admin)).andExpect(status().isConflict());
    }


    @Test
    void approvalReturnsExactlyOneDeliveredLinkAndNeverIncludesItInGeneralReads() throws Exception {
        Long id = submit(nextCpf());
        String link = json(approve(id).andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store")), "$.activationLink");
        verify(mail).sendActivationLink(anyString(), eq("Candidate Jeep Club"), eq(link));
        String raw = deliveredToken(1);
        assertThat(tokens.findByTokenHash(hashes.hash(raw))).isPresent();
        assertThat(tokens.findByTokenHash(raw)).isEmpty();
        mvc.perform(get("/admin/membership-applications/{id}", id).header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activationLink").doesNotExist()).andExpect(jsonPath("$.token").doesNotExist());
        mvc.perform(get("/admin/membership-applications").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(raw))));
        assertThat(reviews.findByApplicationIdOrderById(id)).singleElement().satisfies(row -> {
            assertThat(row.getOperation()).isEqualTo("APPROVE_ACCESS_LINK");
            assertThat(row.getPreviousStatus()).isEqualTo("PENDING");
            assertThat(row.getActorId()).isEqualTo(adminId);
        });
    }

    @Test
    void reversingResentLinkRemovesOnlyPendingProvisioningAndPreservesApprovalEvidence() throws Exception {
        Long id=submit(nextCpf());approve(id).andExpect(status().isOk());String old=deliveredToken(1);
        var before=applications.findById(id).orElseThrow();Long user=before.getCreatedUserId();
        Role retainedRole=roles.save(Role.create("reversal-history-"+id,null,Instant.now(clock)));
        userRoles.save(UserRole.create(user,retainedRole.getId(),Instant.now(clock)));
        new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(status ->
                invitations.save(com.jeepclub.backend.iam.authentication.core.domain.model.AccountActivationInvitation.create(
                        user,hashes.hash("synthetic-admin-invitation-"+id),Instant.now(clock).plusSeconds(3600))));
        mvc.perform(post("/admin/membership-applications/{id}/activation-link/resend",id).header("Authorization",admin)).andExpect(status().isNoContent());
        String fresh=deliveredToken(2);
        reject(id,false).andExpect(status().isNoContent());
        assertThat(identities.findById(user)).isEmpty();assertThat(accounts.findById(user)).isEmpty();
        assertThat(invitations.findById(user).orElseThrow().deliveryStatus()).isEqualTo("INVALIDATED");
        assertThat(roles.findById(retainedRole.getId())).isPresent();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM authorization_user_roles WHERE user_id=?",Long.class,user)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM authorization_user_role_removal_history WHERE user_id=?",Long.class,user)).isEqualTo(1);
        var rejected=applications.findById(id).orElseThrow();
        assertThat(rejected.getStatus()).isEqualTo(MembershipApplicationStatus.REJECTED);
        assertThat(rejected.getCreatedUserId()).isEqualTo(user);
        assertThat(tokens.findByTokenHash(hashes.hash(old)).orElseThrow().getUsedAt()).isNotNull();
        assertThat(tokens.findByTokenHash(hashes.hash(fresh)).orElseThrow().getUsedAt()).isNotNull();
        complete(old).andExpect(status().isConflict());complete(fresh).andExpect(status().isConflict());
        mvc.perform(post("/admin/membership-applications/{id}/activation-link/resend",id).header("Authorization",admin)).andExpect(status().isConflict());
        reject(id,false).andExpect(status().isConflict());approve(id).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM identity_user_deletion_history WHERE original_id=?",Long.class,user)).isEqualTo(1);
        assertThat(reviews.findByApplicationIdOrderById(id)).hasSize(2).last().satisfies(row -> {
            assertThat(row.getPreviousStatus()).isEqualTo("APPROVED");assertThat(row.getResultingStatus()).isEqualTo("REJECTED");
            assertThat(row.getPreviousReviewerId()).isEqualTo(before.getReviewedByUserId());
            assertThat(row.getPreviousReviewedAt()).isEqualTo(before.getReviewedAt());assertThat(row.getOriginalUserId()).isEqualTo(user);
        });
        verify(mail).sendRejectionNotice(anyString(),eq("Candidate Jeep Club"),eq("Administrative review"));
    }

    @Test
    void temporaryPasswordReversalInvalidatesItsAlreadyIssuedPasswordChangeChallenge() throws Exception {
        String cpf=nextCpf();Long id=submit(cpf);
        String temporary=json(mvc.perform(post("/admin/membership-applications/{id}/approve/temporary-password",id)
                .header("Authorization",admin)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")),"$.temporaryPassword");
        Long user=applications.findById(id).orElseThrow().getCreatedUserId();
        String challenge=json(login(cpf,temporary).andExpect(status().isOk()),"$.passwordChangeToken");
        reject(id,true).andExpect(status().isNoContent());
        assertThat(identities.findById(user)).isEmpty();assertThat(accounts.findById(user)).isEmpty();
        assertThat(blocks.existsByCpfAndUnblockedAtIsNull(cpf)).isTrue();
        assertThat(mvc.perform(post("/authentication/login/password-change").contentType(MediaType.APPLICATION_JSON)
                .content("{\"passwordChangeToken\":\""+challenge+"\",\"newPassword\":\""+PASSWORD+"\"}"))
                .andReturn().getResponse().getStatus()).isBetween(400,499);
        assertThat(login(cpf,temporary).andReturn().getResponse().getStatus()).isBetween(400,499);
    }

    @Test
    void pendingRejectionAndBlockingKeepTheOriginalNoProvisioningBehavior() throws Exception {
        String plain=nextCpf();Long first=submit(plain);reject(first,false).andExpect(status().isNoContent());
        assertThat(identities.findByCpf(plain)).isEmpty();assertThat(blocks.existsByCpfAndUnblockedAtIsNull(plain)).isFalse();
        String blocked=nextCpf();Long second=submit(blocked);reject(second,true).andExpect(status().isNoContent());
        assertThat(identities.findByCpf(blocked)).isEmpty();assertThat(blocks.existsByCpfAndUnblockedAtIsNull(blocked)).isTrue();
    }

    @Test
    void completedFirstAccessAndRealAccessAndRefreshTokensSurviveRejectedReversalAttempt() throws Exception {
        String cpf=nextCpf();Long id=submit(cpf);approve(id).andExpect(status().isOk());complete(deliveredToken(1)).andExpect(status().isNoContent());
        Long user=applications.findById(id).orElseThrow().getCreatedUserId();
        var authenticated=login(cpf,PASSWORD).andExpect(status().isOk());String access=json(authenticated,"$.accessToken"),refresh=json(authenticated,"$.refreshToken");
        reject(id,true).andExpect(status().isConflict());
        assertThat(accounts.existsById(user)).isTrue();assertThat(blocks.existsByCpfAndUnblockedAtIsNull(cpf)).isFalse();
        assertThat(applications.findById(id).orElseThrow().getStatus()).isEqualTo(MembershipApplicationStatus.COMPLETED);
        mvc.perform(get("/authentication/me").header("Authorization","Bearer "+access)).andExpect(status().isOk());
        mvc.perform(post("/authentication/refresh").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\""+refresh+"\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void inconsistentPermanentCredentialOnApprovedApplicationIsAConflictWithoutRevocation() throws Exception {
        String cpf=nextCpf();Long id=submit(cpf);approve(id).andExpect(status().isOk());
        Long user=applications.findById(id).orElseThrow().getCreatedUserId();
        jdbc.update("UPDATE authentication_accounts SET credential_status='PERMANENT' WHERE identity_id=?",user);
        reject(id,true).andExpect(status().isConflict());
        assertApprovedIntact(id,cpf,deliveredToken(1));
    }

    @Test
    void membershipForeignKeyPreventsReversalWithoutPartialRejectionRevocationOrBlock() throws Exception {
        String cpf=nextCpf();Long id=submit(cpf);approve(id).andExpect(status().isOk());
        Long user=applications.findById(id).orElseThrow().getCreatedUserId();
        jdbc.update("INSERT INTO membership_members(identity_user_id,membership_number,created_at,updated_at,version) VALUES(?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0)",user,"REVERSAL-"+user);
        reject(id,true).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEMBERSHIP_FIRST_ACCESS_CONFLICT"));
        assertApprovedIntact(id,cpf,deliveredToken(1));assertThat(registryQuery.byIdentityUserId(user)).isPresent();
    }

    @Test
    void blockPersistenceFailureAfterDeletionRollsBackUserAccountTokensReviewHistoryAndDeletionHistory() throws Exception {
        String cpf=nextCpf();Long id=submit(cpf);approve(id).andExpect(status().isOk());Long user=applications.findById(id).orElseThrow().getCreatedUserId();
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("synthetic block persistence failure")).when(blocks).save(any());
        try { reject(id,true).andExpect(status().isInternalServerError()); } finally { reset(blocks); }
        assertApprovedIntact(id,cpf,deliveredToken(1));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM identity_user_deletion_history WHERE original_id=?",Long.class,user)).isZero();
        verify(mail,never()).sendRejectionNotice(any(),any(),any());
        reject(id,true).andExpect(status().isNoContent());
    }

    @Test
    void reviewAuditFailureRollsBackApprovalBeforeAnyInvitationIsDelivered() throws Exception {
        String cpf=nextCpf();Long id=submit(cpf);
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("synthetic review persistence failure")).when(reviews).saveAndFlush(any());
        try { approve(id).andExpect(status().isInternalServerError()); } finally { reset(reviews); }
        assertThat(applications.findById(id).orElseThrow().getStatus()).isEqualTo(MembershipApplicationStatus.PENDING);
        assertThat(identities.findByCpf(cpf)).isEmpty();verify(mail,never()).sendActivationLink(any(),any(),any());
    }

    @Test
    void failedRejectionNotificationDoesNotUndoTheCommittedSafeReversal() throws Exception {
        String cpf=nextCpf();Long id=submit(cpf);approve(id).andExpect(status().isOk());
        Long user=applications.findById(id).orElseThrow().getCreatedUserId();
        doThrow(new RuntimeException("synthetic notification failure")).when(mail).sendRejectionNotice(any(),any(),any());
        reject(id,false).andExpect(status().isNoContent());
        assertThat(applications.findById(id).orElseThrow().getStatus()).isEqualTo(MembershipApplicationStatus.REJECTED);
        assertThat(identities.findById(user)).isEmpty();assertThat(accounts.findById(user)).isEmpty();
    }

    @Test
    void aMissingAuthenticationAccountCannotBeMistakenForPendingFirstAccess() throws Exception {
        String cpf=nextCpf();Long id=submit(cpf);approve(id).andExpect(status().isOk());
        Long user=applications.findById(id).orElseThrow().getCreatedUserId();accounts.deleteById(user);
        reject(id,true).andExpect(status().isConflict());
        assertThat(applications.findById(id).orElseThrow().getStatus()).isEqualTo(MembershipApplicationStatus.APPROVED);
        assertThat(identities.findById(user)).isPresent();assertThat(blocks.existsByCpfAndUnblockedAtIsNull(cpf)).isFalse();
    }


    @Test
    void financialAssignmentPreventsReversalAndPreservesEveryFinancialReference() throws Exception {
        String cpf=nextCpf();Long id=submit(cpf);approve(id).andExpect(status().isOk());Long user=applications.findById(id).orElseThrow().getCreatedUserId();
        Long definition=definitions.create("Reversal retained finance "+id,null,new java.math.BigDecimal("125.50"),
                com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType.ONE_TIME,true,
                com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy.AFTER_DUE_DATE,null).id();
        assignments.assignToUser(definition,user);
        reject(id,true).andExpect(status().isConflict());assertApprovedIntact(id,cpf,deliveredToken(1));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM billing_user_charge_assignments WHERE user_id=?",Long.class,user)).isEqualTo(1);
    }

    @Test
    void unexpectedSessionHistoryBlocksReversalInsteadOfDestroyingAuthenticationEvidence() throws Exception {
        String cpf=nextCpf();Long id=submit(cpf);approve(id).andExpect(status().isOk());Long user=applications.findById(id).orElseThrow().getCreatedUserId();
        jdbc.update("INSERT INTO authentication_sessions(user_id,created_at,expires_at,session_status) VALUES(?,CURRENT_TIMESTAMP,?,'ACTIVE')",user,java.sql.Timestamp.from(Instant.now(clock).plusSeconds(3600)));
        reject(id,true).andExpect(status().isConflict());assertApprovedIntact(id,cpf,deliveredToken(1));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM authentication_sessions WHERE user_id=?",Long.class,user)).isEqualTo(1);
    }

    @Test
    void concurrentApprovalCreatesOneIdentityOneInvitationAndOneReview() throws Exception {
        String cpf=nextCpf();Long id=submit(cpf);
        var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
        var start=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.Callable<String> approve=() -> {
            await(start);
            try { administration.approveWithAccessLink(id,adminId);return "APPROVED"; }
            catch(com.jeepclub.backend.memberships.core.application.exception.MembershipApplicationAlreadyProcessedException conflict) { return "CONFLICT"; }
        };
        try {
            var first=executor.submit(approve);var second=executor.submit(approve);start.countDown();
            assertThat(java.util.List.of(first.get(20,java.util.concurrent.TimeUnit.SECONDS),second.get(20,java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("APPROVED","CONFLICT");
            assertThat(identities.findByCpf(cpf)).isPresent();assertThat(reviews.findByApplicationIdOrderById(id)).hasSize(1);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM membership_activation_tokens WHERE application_id=?",Long.class,id)).isEqualTo(1);
            verify(mail).sendActivationLink(any(),any(),any());
        } finally { start.countDown();executor.shutdownNow(); }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"true,false","false,false","true,true","false,true"})
    void firstAccessAndRejectionSerializeOnIdentityWithoutDeletingAnActivatedAccount(boolean accessWins,boolean temporary) throws Exception {
        String cpf=nextCpf();Long id=submit(cpf);String credential;
        if(temporary) {
            String password=json(mvc.perform(post("/admin/membership-applications/{id}/approve/temporary-password",id)
                    .header("Authorization",admin)).andExpect(status().isOk()),"$.temporaryPassword");
            credential=json(login(cpf,password).andExpect(status().isOk()),"$.passwordChangeToken");
        } else { approve(id).andExpect(status().isOk());credential=deliveredToken(1); }
        Long user=applications.findById(id).orElseThrow().getCreatedUserId();
        Runnable finish=() -> { if(temporary)authentication.completeRequiredPasswordChange(credential,PASSWORD);else activation.complete(credential,PASSWORD); };
        Runnable reject=() -> administration.reject(id,adminId,"Concurrent review");
        var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
        var applied=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
        try {
            var winner=executor.submit(() -> new org.springframework.transaction.support.TransactionTemplate(transactionManager).execute(status -> {
                identityLock.byId(user).orElseThrow();(accessWins?finish:reject).run();applied.countDown();await(release);return "COMMITTED";
            }));
            assertThat(applied.await(20,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            var loser=executor.submit(() -> {
                try { (accessWins?reject:finish).run();return "UNEXPECTED_SUCCESS"; }
                catch(RuntimeException conflict) { return conflict; }
            });
            assertThatThrownBy(() -> loser.get(200,java.util.concurrent.TimeUnit.MILLISECONDS)).isInstanceOf(java.util.concurrent.TimeoutException.class);
            release.countDown();assertThat(winner.get(20,java.util.concurrent.TimeUnit.SECONDS)).isEqualTo("COMMITTED");
            Object rejectedOperation=loser.get(20,java.util.concurrent.TimeUnit.SECONDS);
            assertThat(rejectedOperation).isInstanceOfAny(
                    com.jeepclub.backend.memberships.core.application.exception.MembershipApplicationAlreadyProcessedException.class,
                    com.jeepclub.backend.memberships.core.application.exception.MembershipFirstAccessConflictException.class,
                    com.jeepclub.backend.iam.authentication.core.application.exceptions.login.PasswordChangeChallengeInvalidException.class);
            assertThat(applications.findById(id).orElseThrow().getStatus()).isEqualTo(accessWins?MembershipApplicationStatus.COMPLETED:MembershipApplicationStatus.REJECTED);
            if(accessWins) {
                assertThat(identities.findById(user)).isPresent();assertThat(accounts.findById(user).orElseThrow().getCredentialStatus()).isEqualTo(CredentialStatus.PERMANENT);
                assertThat(registryQuery.byIdentityUserId(user)).isPresent();
            } else { assertThat(identities.findById(user)).isEmpty();assertThat(accounts.findById(user)).isEmpty();assertThat(registryQuery.byIdentityUserId(user)).isEmpty(); }
        } finally { release.countDown();executor.shutdownNow(); }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={true,false})
    void resendAndRejectionCannotLeaveAUsableInvitationAfterReversal(boolean resendWins) throws Exception {
        String cpf=nextCpf();Long id=submit(cpf);approve(id).andExpect(status().isOk());Long user=applications.findById(id).orElseThrow().getCreatedUserId();
        Runnable resend=() -> administration.resendActivationLink(id),reject=() -> administration.reject(id,adminId,null);
        var executor=java.util.concurrent.Executors.newFixedThreadPool(2);var applied=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
        try {
            var winner=executor.submit(() -> new org.springframework.transaction.support.TransactionTemplate(transactionManager).execute(status -> {
                identityLock.byId(user).orElseThrow();(resendWins?resend:reject).run();applied.countDown();await(release);return true;
            }));
            assertThat(applied.await(20,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            var loser=executor.submit(() -> { try { (resendWins?reject:resend).run();return "SUCCESS"; } catch(RuntimeException conflict) { return "CONFLICT"; } });
            assertThatThrownBy(() -> loser.get(200,java.util.concurrent.TimeUnit.MILLISECONDS)).isInstanceOf(java.util.concurrent.TimeoutException.class);
            release.countDown();assertThat(winner.get(20,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(loser.get(20,java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(resendWins?"SUCCESS":"CONFLICT");
            assertThat(applications.findById(id).orElseThrow().getStatus()).isEqualTo(MembershipApplicationStatus.REJECTED);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM membership_activation_tokens WHERE application_id=? AND used_at IS NULL",Long.class,id)).isZero();
            assertThat(identities.findById(user)).isEmpty();
        } finally { release.countDown();executor.shutdownNow(); }
    }

    private static void await(java.util.concurrent.CountDownLatch latch) {
        try { if(!latch.await(20,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("Synthetic race fixture timed out"); }
        catch(InterruptedException interrupted) { Thread.currentThread().interrupt();throw new IllegalStateException(interrupted); }
    }

    private void assertApprovedIntact(Long id,String cpf,String raw) {
        Long user=applications.findById(id).orElseThrow().getCreatedUserId();
        assertThat(applications.findById(id).orElseThrow().getStatus()).isEqualTo(MembershipApplicationStatus.APPROVED);
        assertThat(identities.findById(user)).isPresent();assertThat(accounts.existsById(user)).isTrue();
        assertThat(tokens.findByTokenHash(hashes.hash(raw)).orElseThrow().getUsedAt()).isNull();
        assertThat(blocks.existsByCpfAndUnblockedAtIsNull(cpf)).isFalse();assertThat(reviews.findByApplicationIdOrderById(id)).hasSize(1);
    }

    private ResultActions reject(Long id,boolean block) throws Exception {
        return mvc.perform(post("/admin/membership-applications/{id}/"+(block?"reject-and-block":"reject"),id)
                .header("Authorization",admin).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Administrative review\"}"));
    }

    private String nextCpf() {
        String base=Integer.toString(CANDIDATES.incrementAndGet());
        for(int length=9;length<11;length++) {
            int sum=0;for(int i=0;i<length;i++)sum+=(base.charAt(i)-'0')*(length+1-i);
            int remainder=sum%11;base+=(remainder<2?0:11-remainder);
        }
        return base;
    }

    private Long submit(String cpf) throws Exception {
        return ((Number) JsonPath.read(mvc.perform(post("/membership-applications")
                .contentType(MediaType.APPLICATION_JSON).content(submission(cpf)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.created").value(true))
                .andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    private String submission(String cpf) {
        return "{\"name\":\"Candidate Jeep Club\",\"cpf\":\"" + cpf
                + "\",\"email\":\"candidate-" + cpf + "@example.com\",\"phoneNumber\":\"11988887777\"}";
    }

    private ResultActions approve(Long id) throws Exception {
        return mvc.perform(post("/admin/membership-applications/{id}/approve/access-link", id).header("Authorization", admin));
    }

    private String deliveredToken(int count) {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mail, times(count)).sendActivationLink(anyString(), eq("Candidate Jeep Club"), captor.capture());
        return org.springframework.web.util.UriComponentsBuilder.fromUriString(captor.getAllValues().get(count - 1)).build().getQueryParams().getFirst("token");
    }

    private ResultActions complete(String token) throws Exception {
        return mvc.perform(post("/membership-applications/activate").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"" + PASSWORD + "\"}"));
    }

    private ResultActions login(String cpf, String password) throws Exception {
        return mvc.perform(post("/authentication/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"cpf\":\"" + cpf + "\",\"senha\":\"" + password + "\"}"));
    }

    private ResultActions changePassword(String challenge) throws Exception {
        return mvc.perform(post("/authentication/login/password-change").contentType(MediaType.APPLICATION_JSON)
                .content("{\"passwordChangeToken\":\""+challenge+"\",\"newPassword\":\""+PASSWORD+"\"}"));
    }

    private java.util.Map<String,Object> registrySnapshot() {
        var snapshot=new java.util.LinkedHashMap<String,Object>();
        for(String table:java.util.List.of("membership_members","membership_number_claims","membership_member_audits")) {
            snapshot.put(table,jdbc.queryForObject("select count(*) from "+table,Long.class));
        }
        snapshot.put("sequence",jdbc.queryForList("select next_value,format_signature,version from membership_registry_sequence where id=1"));
        return snapshot;
    }

    private String json(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    }
}
