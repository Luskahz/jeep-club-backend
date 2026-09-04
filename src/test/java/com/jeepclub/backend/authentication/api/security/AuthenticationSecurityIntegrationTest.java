package com.jeepclub.backend.authentication.api.security;

import com.jeepclub.backend.iam.authorization.core.domain.model.Role;
import com.jeepclub.backend.iam.authorization.core.domain.model.RolePermission;
import com.jeepclub.backend.iam.authorization.core.domain.model.UserRole;
import com.jeepclub.backend.iam.authorization.core.repository.PermissionRepository;
import com.jeepclub.backend.iam.authorization.core.repository.RolePermissionRepository;
import com.jeepclub.backend.iam.authorization.core.repository.RoleRepository;
import com.jeepclub.backend.iam.authorization.core.repository.UserRoleRepository;
import com.jeepclub.backend.iam.identity.api.module.UserAuthenticationTokens;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import com.jeepclub.backend.iam.identity.api.module.UserRegistration;
import com.jeepclub.backend.iam.identity.api.module.UserRegistrationData;
import com.jeepclub.backend.shared.authorization.PermissionCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired private UserRegistration userRegistration;
    @Autowired private UserQuery userQuery;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private RolePermissionRepository rolePermissionRepository;
    @Autowired private UserRoleRepository userRoleRepository;

    @Test
    void publicAuthenticationRoutesReachTheirControllersAnonymously() throws Exception {
        assertPublicValidationRoute("/authentication/login", "{}");
        assertPublicValidationRoute("/identity/register", "{}");
        assertPublicValidationRoute("/authentication/refresh", "{}");
        assertPublicValidationRoute("/authentication/login/password-change", "{}");
    }

    @Test
    void publicRecoveryRoutesAreNotInterceptedBySecurity() throws Exception {
        String cpf = "{\"cpf\":\"52998224725\"}";
        mockMvc.perform(post("/authentication/password-recovery/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cpf))
                .andExpect(status().isOk());
        mockMvc.perform(post("/authentication/password-recovery/requests/email-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cpf))
                .andExpect(status().isOk());
        assertPublicValidationRoute(
                "/authentication/password-recovery/requests/token/reset",
                "{}"
        );
    }

    @Test
    void authenticatedAndAdministrativeRoutesRemainProtected() throws Exception {
        mockMvc.perform(get("/authentication/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        mockMvc.perform(get("/identity/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        mockMvc.perform(get("/authorization/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        mockMvc.perform(post("/authentication/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        mockMvc.perform(get("/identity/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        mockMvc.perform(get("/authentication/admin/password-recovery/requests"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @Transactional
    void identityAdminEndpointsEnforceReadDisableAndEnablePermissions() throws Exception {
        Instant now = Instant.parse("2026-09-04T12:00:00Z");
        String actorCpf = "86420975310";
        UserAuthenticationTokens tokens = userRegistration.registerAndAuthenticate(
                new UserRegistrationData(
                        "Security Actor", null, "security-actor@example.com", actorCpf,
                        null, null, null, now
                ),
                "security-password"
        );
        Long actorId = userQuery.findByCpf(actorCpf).orElseThrow().id();
        Long targetId = userRegistration.createWithPermanentCredential(
                new UserRegistrationData(
                        "Security Target", null, "security-target@example.com", "75319086466",
                        null, null, null, now
                ),
                "security-password"
        );
        String bearer = "Bearer " + tokens.accessToken();

        mockMvc.perform(get("/identity/admin/users").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/identity/admin/users/{id}/disable", targetId)
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/identity/admin/users/{id}/enable", targetId)
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isForbidden());

        Role role = roleRepository.save(Role.create("identity-security-test", "Test role", now));
        userRoleRepository.save(UserRole.create(actorId, role.getId(), now));
        grant(role, PermissionCode.IDENTITY_USER_READ, now);

        mockMvc.perform(get("/identity/admin/users").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/identity/admin/users/{id}/disable", targetId)
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isForbidden());

        grant(role, PermissionCode.IDENTITY_USER_DISABLE, now);
        mockMvc.perform(patch("/identity/admin/users/{id}/disable", targetId)
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/identity/admin/users/{id}/enable", targetId)
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isForbidden());

        grant(role, PermissionCode.IDENTITY_USER_ENABLE, now);
        mockMvc.perform(patch("/identity/admin/users/{id}/enable", targetId)
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk());
    }

    @Test
    void invalidBearerTokenUsesLocalizedProblemDetail() throws Exception {
        mockMvc.perform(get("/authentication/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"))
                .andExpect(jsonPath("$.detail").value("The access token is invalid or has expired."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void openApiKeepsPublishedAuthenticationPaths() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/authentication/login']['post']").exists())
                .andExpect(jsonPath("$['paths']['/identity/register']['post']").exists())
                .andExpect(jsonPath("$['paths']['/authentication/register']").doesNotExist())
                .andExpect(jsonPath("$['paths']['/authentication/refresh']['post']").exists())
                .andExpect(jsonPath("$['paths']['/authentication/me']['get']").exists())
                .andExpect(jsonPath("$['paths']['/identity/me']['get']").exists())
                .andExpect(jsonPath("$['paths']['/authorization/me']['get']").exists())
                .andExpect(jsonPath("$['paths']['/authentication/password-recovery/requests']['post']").exists())
                .andExpect(jsonPath("$['paths']['/identity/admin/users']['get']").exists())
                .andExpect(jsonPath("$['paths']['/identity/admin/users/{userId}']['get']").exists())
                .andExpect(jsonPath("$['paths']['/identity/admin/users/{userId}/disable']['patch']").exists())
                .andExpect(jsonPath("$['paths']['/identity/admin/users/{userId}/enable']['patch']").exists())
                .andExpect(jsonPath("$['paths']['/authentication/admin/users']").doesNotExist())
                .andExpect(jsonPath("$['components']['schemas']['UserRegistrationRequest']['properties']['cpf']['pattern']")
                        .value("^(\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2})$"))
                .andExpect(jsonPath("$['components']['schemas']['UserRegistrationRequest']['properties']['birthData']")
                        .doesNotExist())
                .andExpect(jsonPath("$['components']['schemas']['AdminUserResponse']['required']").doesNotExist())
                .andExpect(jsonPath("$['components']['schemas']['AdminUserResponse']['properties']['profilePhotoUrl']['format']")
                        .doesNotExist())
                .andExpect(jsonPath("$['components']['schemas']['AdminUserResponse']['properties']['id']").exists())
                .andExpect(jsonPath("$['components']['schemas']['AdminUserResponse']['properties']['name']").exists())
                .andExpect(jsonPath("$['components']['schemas']['AdminUserResponse']['properties']['birthDate']").exists())
                .andExpect(jsonPath("$['components']['schemas']['AdminUserResponse']['properties']['email']").exists())
                .andExpect(jsonPath("$['components']['schemas']['AdminUserResponse']['properties']['cpf']").exists())
                .andExpect(jsonPath("$['components']['schemas']['AdminUserResponse']['properties']['rg']").exists())
                .andExpect(jsonPath("$['components']['schemas']['AdminUserResponse']['properties']['phoneNumber']").exists())
                .andExpect(jsonPath("$['components']['schemas']['AdminUserResponse']['properties']['profilePhotoUrl']").exists())
                .andExpect(jsonPath("$['components']['schemas']['AdminUserResponse']['properties']['status']").exists())
                .andExpect(jsonPath("$['components']['schemas']['AdminUserResponse']['properties']['createdAt']").exists())
                .andExpect(jsonPath("$['components']['schemas']['AdminUserResponse']['properties']['disabledAt']").exists())
                .andExpect(jsonPath("$['components']['schemas']['AdminUserResponse']['properties']['updatedAt']").exists());
    }

    private void assertPublicValidationRoute(String path, String body) throws Exception {
        mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    private void grant(Role role, PermissionCode code, Instant now) {
        Long permissionId = permissionRepository.findByCode(code).orElseThrow().getId();
        rolePermissionRepository.save(RolePermission.create(role.getId(), permissionId, now));
    }
}
