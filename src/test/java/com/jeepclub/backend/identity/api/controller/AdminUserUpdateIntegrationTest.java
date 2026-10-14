package com.jeepclub.backend.identity.api.controller;

import com.jeepclub.backend.iam.identity.api.module.*;
import com.jeepclub.backend.iam.identity.core.application.service.user.AdminUserService;
import com.jeepclub.backend.iam.identity.core.repository.UserRepository;
import com.jeepclub.backend.iam.identity.api.http.dto.admin.user.AdminUpdateUserRequestDTO;
import com.jeepclub.backend.iam.authorization.core.domain.model.*;
import com.jeepclub.backend.iam.authorization.core.repository.*;
import com.jeepclub.backend.iam.authentication.core.application.service.session.SessionService;
import com.jeepclub.backend.iam.authentication.core.application.service.activation.AccountActivationInvitationService;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.login.InvalidCredentialsException;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.account.ActivationInvitationException;
import com.jeepclub.backend.iam.authentication.infra.mail.AccountActivationMailSender;
import com.jeepclub.backend.iam.authentication.infra.persistence.jpa.AccountActivationInvitationJpaRepository;
import com.jeepclub.backend.platform.logging.SystemLogJpaRepository;
import com.jeepclub.backend.shared.authorization.PermissionCode;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;
import java.time.Clock;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminUserUpdateIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRegistration registration;
    @Autowired UserQuery users;
    @Autowired AdminUserService admin;
    @Autowired UserRepository repository;
    @Autowired EntityManager em;
    @Autowired Clock clock;
    @Autowired PermissionRepository permissions;
    @Autowired RoleRepository roles;
    @Autowired RolePermissionRepository rolePermissions;
    @Autowired UserRoleRepository userRoles;
    @Autowired SystemLogJpaRepository logs;
    @Autowired JsonMapper json;
    @Autowired Validator validator;
    @Autowired SessionService sessions;
    @Autowired AccountActivationInvitationService invitations;
    @Autowired AccountActivationInvitationJpaRepository invitationRows;
    @MockitoBean AccountActivationMailSender mail;
    UserDetails before;
    Long actorId, otherId;
    String token;
    Role role;

    @org.junit.jupiter.api.io.TempDir static java.nio.file.Path mediaRoot;
    @org.springframework.test.context.DynamicPropertySource
    static void storage(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("storage.local.root-directory", () -> mediaRoot.toString());
    }

    @BeforeEach void setup() {
        var now = clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        token = registration.registerAndAuthenticate(new UserRegistrationData("Administrator", null,
                "actor@example.com", "39053344705", null, null, null, now), "password-123").accessToken();
        actorId = users.findByCpf("39053344705").orElseThrow().id();
        Long target = registration.createWithPermanentCredential(new UserRegistrationData("Original", LocalDate.of(2000, 1, 1),
                "original@example.com", "52998224725", "12345", "11999999999", null, now), "password-123");
        before = users.findById(target).orElseThrow();
        otherId = registration.createWithPermanentCredential(new UserRegistrationData("Other", null,
                "other@example.com", "16899535009", "99999", null, null, now), "password-123");
        role = roles.save(Role.create("test-editor", null, now));
        userRoles.save(UserRole.create(actorId, role.getId(), now));
        grant(PermissionCode.IDENTITY_USER_UPDATE);
        grant(PermissionCode.IDENTITY_USER_READ);
    }

    void grant(PermissionCode code) {
        rolePermissions.save(RolePermission.create(role.getId(), permissions.findByCode(code).orElseThrow().getId(), clock.instant()));
    }
    org.springframework.test.web.servlet.ResultActions patchUser(String body) throws Exception {
        return mvc.perform(patch("/identity/admin/users/{id}", before.id()).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test void editsAllScalarFieldsPreservesIdentityAndAuditsOnlyNames() throws Exception {
        patchUser("""
                {"name":"  New Name  ","birthDate":"1999-12-31","email":" NEW@example.COM ",
                 "cpf":"111.444.777-35","rg":"22.333-4","phoneNumber":"(11) 98888-7777"}
                """).andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.id").value(before.id())).andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.email").value("new@example.com")).andExpect(jsonPath("$.cpf").value("11144477735"))
                .andExpect(jsonPath("$.rg").value("223334")).andExpect(jsonPath("$.phoneNumber").value("11988887777"))
                .andExpect(jsonPath("$.birthDate").value("1999-12-31")).andExpect(jsonPath("$.status").value("ACTIVE"));
        em.clear();
        assertThat(users.findById(before.id()).orElseThrow().createdAt()).isEqualTo(before.createdAt());
        assertThat(users.findById(otherId).orElseThrow().name()).isEqualTo("Other");
        var changes = logs.findAll().stream().filter(l -> l.getAction().startsWith("IDENTITY_USER_UPDATE:")).toList();
        assertThat(changes).hasSize(6).allSatisfy(l -> {
            assertThat(l.getActorId()).isEqualTo(actorId);
            assertThat(l.getPath()).isEqualTo("/identity/admin/users/" + before.id());
            assertThat(l.getAction()).doesNotContain("11144477735", "new@example.com", "New Name");
        });
        assertThatThrownBy(() -> sessions.login(before.cpf(), "password-123")).isInstanceOf(InvalidCredentialsException.class);
        assertThat(sessions.login("11144477735", "password-123")).isNotNull();
    }

    @Test void noOpAndOmissionPreserveValuesAndNullRemovesOptionals() throws Exception {
        long count = logs.count();
        patchUser("{}").andExpect(status().isOk());
        patchUser("{\"name\":\" Original \"}").andExpect(status().isOk());
        em.clear();
        assertThat(users.findById(before.id()).orElseThrow()).isEqualTo(before);
        assertThat(logs.count()).isEqualTo(count);
        patchUser("{\"name\":\"New\"}").andExpect(status().isOk()).andExpect(jsonPath("$.cpf").value(before.cpf()))
                .andExpect(jsonPath("$.rg").value(before.rg()));
        patchUser("{\"birthDate\":null,\"email\":null,\"rg\":null,\"phoneNumber\":null,\"profilePhotoStorageKey\":null}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").doesNotExist()).andExpect(jsonPath("$.rg").doesNotExist());
        em.clear();
        var saved = users.findById(before.id()).orElseThrow();
        assertThat(saved.birthDate()).isNull(); assertThat(saved.email()).isNull(); assertThat(saved.phoneNumber()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings={"id","userId","status","createdAt","updatedAt","disabledAt","roles","permissions","password","profilePhotoUrl","unknown"})
    void rejectsForbiddenFieldsWithoutPartialWrites(String field) throws Exception {
        patchUser("{\"name\":\"Do not persist\",\"" + field + "\":null}").andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        em.clear(); assertThat(users.findById(before.id()).orElseThrow()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings={"[]","null","{\"name\":null}","{\"name\":\" \"}","{\"cpf\":null}","{\"cpf\":\"11111111111\"}",
            "{\"cpf\":\"52998224724\"}","{\"cpf\":true}","{\"cpf\":\"x52998224725\"}","{\"email\":\"invalid\"}","{\"email\":12}",
            "{\"birthDate\":\"2026-02-30\"}","{\"rg\":\"letters\"}","{\"phoneNumber\":false}","{\"profilePhotoStorageKey\":\"https://internal/file.png\"}"})
    void rejectsInvalidTypesAndValues(String body) throws Exception {
        patchUser(body).andExpect(status().isBadRequest());
        em.clear(); assertThat(users.findById(before.id()).orElseThrow()).isEqualTo(before);
    }

    @ParameterizedTest @ValueSource(strings={"{\"cpf\":\"16899535009\"}","{\"email\":\"OTHER@example.com\"}","{\"rg\":\"99999\"}"})
    void conflictsAre409(String body) throws Exception {
        patchUser(body).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("USER_UPDATE_CONFLICT"));
        em.clear(); assertThat(users.findById(before.id()).orElseThrow()).isEqualTo(before);
    }

    @Test void missingTargetIs404AndInvalidIdIs400() throws Exception {
        mvc.perform(patch("/identity/admin/users/999999").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isNotFound());
        mvc.perform(patch("/identity/admin/users/0").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
    }

    @Test void anonymousAndReaderCannotWriteAndWriterCanWriteWithoutReadPermission() throws Exception {
        mvc.perform(patch("/identity/admin/users/{id}", before.id()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/identity/admin/users/{id}/profile", before.id())).andExpect(status().isUnauthorized());
        mvc.perform(put("/identity/admin/users/{id}/profile", before.id()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        rolePermissions.deleteByRoleIdAndPermissionId(role.getId(), permissions.findByCode(PermissionCode.IDENTITY_USER_UPDATE).orElseThrow().getId());
        patchUser("{}").andExpect(status().isForbidden());
        mvc.perform(put("/identity/admin/users/{id}/profile", before.id()).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mvc.perform(get("/identity/admin/users/{id}/profile", before.id()).header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        grant(PermissionCode.IDENTITY_USER_UPDATE);
        rolePermissions.deleteByRoleIdAndPermissionId(role.getId(), permissions.findByCode(PermissionCode.IDENTITY_USER_READ).orElseThrow().getId());
        patchUser("{\"name\":\"Writer\"}").andExpect(status().isOk());
        mvc.perform(get("/identity/admin/users/{id}/profile", before.id()).header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }

    @Test void replacesAllNineComplementaryFieldsAndClearsOmittedBlocks() throws Exception {
        mvc.perform(get("/identity/admin/users/{id}/profile", before.id()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.profileCompletionPending").value(true));
        mvc.perform(put("/identity/admin/users/{id}/profile", before.id()).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"workProfile":{"occupation":" Driver ","workplace":" Club "},"address":{"postalCode":"12345-678",
                 "street":" Rua A ","number":"S/N","complement":" House ","neighborhood":"Bairro","city":"Cidade","state":"sp"}}
                """)).andExpect(status().isOk()).andExpect(jsonPath("$.profileCompletionPending").value(false))
                .andExpect(jsonPath("$.workProfile.occupation").value("Driver")).andExpect(jsonPath("$.address.state").value("SP"))
                .andExpect(jsonPath("$.address.postalCode").value("12345678"));
        assertThat(logs.findAll().stream().filter(l -> l.getAction().startsWith("IDENTITY_USER_PROFILE_UPDATE:")).toList()).hasSize(9);
        mvc.perform(put("/identity/admin/users/{id}/profile", before.id()).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"workProfile\":{\"occupation\":\"Pilot\"}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.address").isEmpty()).andExpect(jsonPath("$.workProfile.workplace").isEmpty())
                .andExpect(jsonPath("$.profileCompletionPending").value(true));
        em.clear(); assertThat(users.findById(before.id()).orElseThrow()).isEqualTo(before);
    }

    @Test void rootContactCannotBeTakenOverButRootCanCorrectOwnContactAndOthersCanEditName() throws Exception {
        Role root = roles.save(Role.createRoot("ROOT", null, clock.instant()));
        userRoles.save(UserRole.create(before.id(), root.getId(), clock.instant()));
        patchUser("{\"email\":\"takeover@example.com\"}").andExpect(status().isConflict());
        patchUser("{\"cpf\":\"11144477735\"}").andExpect(status().isConflict());
        patchUser("{\"name\":\"Root name\"}").andExpect(status().isOk());
        admin.update(before.id(), before.id(), AdminUpdateUserRequestDTO.read(json.readTree("{\"email\":\"own@example.com\"}"), validator));
        assertThat(users.findById(before.id()).orElseThrow().email()).isEqualTo("own@example.com");
    }

    @Test void disabledTargetCanBeCorrectedWithoutEnablingAccount() throws Exception {
        admin.disable(before.id());
        patchUser("{\"name\":\"Disabled corrected\",\"cpf\":\"11144477735\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));
        assertThat(users.findById(before.id()).orElseThrow().disabledAt()).isNotNull();
    }

    @Test void photoUsesGlobalMediaAndCanBePreservedOrUnlinkedWithoutDeletingBytes() throws Exception {
        byte[] png = {(byte)137, 80, 78, 71, 13, 10, 26, 10};
        var response = mvc.perform(multipart("/media/images")
                .file(new org.springframework.mock.web.MockMultipartFile("file", "photo.png", "image/png", png))
                .header("Authorization", "Bearer " + token)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String key=json.readTree(response).get("storageKey").asString();
        patchUser("{\"profilePhotoStorageKey\":\"" + key + "\"}").andExpect(status().isOk()).andExpect(jsonPath("$.profilePhotoStorageKey").value(key));
        patchUser("{\"name\":\"Photo retained\"}").andExpect(status().isOk()).andExpect(jsonPath("$.profilePhotoStorageKey").value(key));
        patchUser("{\"profilePhotoStorageKey\":null}").andExpect(status().isOk()).andExpect(jsonPath("$.profilePhotoStorageKey").doesNotExist());
        mvc.perform(get("/media/images").param("key",key).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(content().bytes(png));
    }

    @Test void missingPhotoNeverPartiallyPersistsAndUnknownProfileFieldsAreRejected() throws Exception {
        patchUser("{\"name\":\"Never persists\",\"profilePhotoStorageKey\":\"images/2026/10/14/550e8400-e29b-41d4-a716-446655440000.png\"}")
                .andExpect(status().isNotFound());
        mvc.perform(put("/identity/admin/users/{id}/profile", before.id()).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"address\":{\"state\":\"ZZ\"}}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/identity/admin/users/{id}/profile", before.id()).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":1}"))
                .andExpect(status().isBadRequest());
        em.clear(); assertThat(users.findById(before.id()).orElseThrow()).isEqualTo(before);
    }

    @Test void emailChangeInvalidatesInvitationAndOldQueuedDeliveryCannotRestoreIt() throws Exception {
        var created = registration.createAdministratively(new UserRegistrationData("Pending", null, "pending@example.com", "11144477735", null, null, null, clock.instant()), UserActivationMethod.EMAIL_INVITATION, actorId);
        var invitation = invitationRows.findById(created.userId()).orElseThrow();
        String hash = invitation.getTokenHash();
        admin.update(created.userId(), actorId, AdminUpdateUserRequestDTO.read(json.readTree("{\"email\":\"correct@example.com\"}"), validator));
        em.flush(); em.clear();
        assertThat(invitationRows.findById(created.userId()).orElseThrow().getDeliveryStatus()).isEqualTo("INVALIDATED");
        assertThat(invitationRows.updateDeliveryStatusIfCurrent(created.userId(), hash, "SENT")).isZero();
        invitations.issueForExistingUser(created.userId(), actorId);
        em.flush(); em.clear();
        assertThat(invitationRows.findById(created.userId()).orElseThrow().getTokenHash()).isNotEqualTo(hash);
    }
}
