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
@SpringBootTest
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
    @MockitoSpyBean MembershipApplicationRepository applications;
    @MockitoBean MemberActivationMailSender mail;
    private String admin;

    @BeforeEach
    void administratorWithRealCredentialsAndApprovalPermission() throws Exception {
        String cpf = "73045198279";
        if (!identities.existsByCpf(cpf)) {
            Long id = registration.createWithPermanentCredential(new UserRegistrationData(
                    "Onboarding Admin", null, null, cpf, null, null, null, Instant.now(clock)), PASSWORD);
            Role role = roles.save(Role.create("onboarding-audit", "Audit", Instant.now(clock)));
            userRoles.save(UserRole.create(id, role.getId(), Instant.now(clock)));
            var permission = permissions.findByCode(com.jeepclub.backend.shared.authorization.PermissionCode.MEMBERSHIP_MEMBERSHIP_REQUEST_APPROVE).orElseThrow();
            rolePermissions.save(RolePermission.create(role.getId(), permission.getId(), Instant.now(clock)));
        }
        admin = "Bearer " + json(login(cpf, PASSWORD).andExpect(status().isOk()), "$.accessToken");
    }

    @Test
    void completeDeterministicOnboardingFlow() throws Exception {
        Long id = submit("52998224725");
        mvc.perform(post("/membership-applications").contentType(MediaType.APPLICATION_JSON)
                        .content(submission("52998224725")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.created").value(false)).andExpect(jsonPath("$.email").doesNotExist());
        approve(id).andExpect(status().isOk());
        var approved = applications.findById(id).orElseThrow();
        Long userId = approved.getCreatedUserId();
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
        var account = accounts.findById(userId).orElseThrow();
        assertThat(account.getCredentialStatus()).isEqualTo(CredentialStatus.PERMANENT);
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
        Long id = submit("16899535009");
        approve(id).andExpect(status().isOk());
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
    }

    @Test
    void expiredAndUnknownTokensDoNotChangeCredentialsOrMembership() throws Exception {
        Long id = submit("11144477735");
        approve(id).andExpect(status().isOk());
        String raw = deliveredToken(1);
        var token = tokens.findByTokenHash(hashes.hash(raw)).orElseThrow();
        token.setExpiresAt(Instant.now(clock));
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
        approve(id).andExpect(status().isOk());
        assertThat(identities.findByCpf("12345678909")).isPresent();
        assertThat(deliveredToken(1)).isNotBlank();
    }

    @Test
    void finalMembershipWriteFailureRollsBackRealCredentialChangeAndTokenConsumption() throws Exception {
        Long id = submit("98765432100");
        approve(id).andExpect(status().isOk());
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
        return captor.getAllValues().get(count - 1);
    }

    private ResultActions complete(String token) throws Exception {
        return mvc.perform(post("/membership-applications/activate").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"" + PASSWORD + "\"}"));
    }

    private ResultActions login(String cpf, String password) throws Exception {
        return mvc.perform(post("/authentication/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"cpf\":\"" + cpf + "\",\"senha\":\"" + password + "\"}"));
    }

    private String json(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    }
}
