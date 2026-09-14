package com.jeepclub.backend.identity.api.contract;

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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IdentityOpenApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRegistration userRegistration;
    @Autowired private UserQuery userQuery;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private RolePermissionRepository rolePermissionRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private Clock clock;

    @Test
    void openApiDescribesIdentityContractsPrecisely() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/identity/register']['post']['responses']['201']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/UserAuthenticationTokenResponse"))
                .andExpect(jsonPath("$['paths']['/identity/register']['post']['responses']['400']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/identity/me']['get']['responses']['404']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['components']['schemas']['UserRegistrationRequest']['properties']['birthDate']['format']")
                        .value("date"))
                .andExpect(jsonPath("$['components']['schemas']['UserRegistrationRequest']['properties']['birthData']")
                        .doesNotExist())
                .andExpect(jsonPath("$['paths']['/identity/admin/users']['get']['x-required-permissions'][0]")
                        .value("IDENTITY_USER_READ"))
                .andExpect(jsonPath("$['paths']['/identity/admin/users']['get']['parameters'][?(@.name == 'page')]")
                        .isArray())
                .andExpect(jsonPath("$['paths']['/identity/admin/users']['get']['parameters'][?(@.name == 'size')]")
                        .isArray())
                .andExpect(jsonPath("$['paths']['/identity/admin/users']['get']['parameters'][?(@.name == 'sort')]")
                        .isArray())
                .andExpect(jsonPath("$['paths']['/identity/admin/users']['get']['parameters'][?(@.name == 'fields')]")
                        .isArray())
                .andExpect(jsonPath("$['paths']['/identity/admin/users']['get']['parameters'][?(@.name == 'birthDate')]")
                        .isArray())
                .andExpect(jsonPath("$['paths']['/identity/admin/users']['get']['responses']['200']['content']['application/json']['schema']['$ref']")
                        .value("#/components/schemas/PageAdminUserResponse"))
                .andExpect(jsonPath("$['components']['schemas']['PageAdminUserResponse']['properties']['content']['type']")
                        .value("array"))
                .andExpect(jsonPath("$['components']['schemas']['PageAdminUserResponse']['properties']['content']['items']['$ref']")
                        .value("#/components/schemas/AdminUserResponse"))
                .andExpect(jsonPath("$['paths']['/identity/admin/users']['get']['responses']['400']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/identity/admin/users/{userId}']['get']['responses']['400']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/identity/admin/users/{userId}/disable']['patch']['x-required-permissions'][0]")
                        .value("IDENTITY_USER_DISABLE"))
                .andExpect(jsonPath("$['paths']['/identity/admin/users/{userId}/disable']['patch']['responses']['400']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/identity/admin/users/{userId}/enable']['patch']['x-required-permissions'][0]")
                        .value("IDENTITY_USER_ENABLE"))
                .andExpect(jsonPath("$['paths']['/identity/admin/users/{userId}/enable']['patch']['responses']['400']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"));
    }

    @Test
    @Transactional
    void positivePathVariableProducesRfcBadRequest() throws Exception {
        Instant now = Instant.now(clock);
        String cpf = "34790621854";
        UserAuthenticationTokens tokens = userRegistration.registerAndAuthenticate(
                new UserRegistrationData(
                        "Identity Validation", null, "identity-validation@example.com", cpf,
                        null, null, null, now
                ),
                "security-password"
        );
        Long userId = userQuery.findByCpf(cpf).orElseThrow().id();
        Role role = roleRepository.save(Role.create("identity-validation", "Test role", now));
        userRoleRepository.save(UserRole.create(userId, role.getId(), now));
        grant(role, PermissionCode.IDENTITY_USER_READ, now);

        mockMvc.perform(get("/identity/admin/users/{userId}", 0)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("HTTP_400"));
    }

    private void grant(Role role, PermissionCode code, Instant now) {
        Long permissionId = permissionRepository.findByCode(code).orElseThrow().getId();
        rolePermissionRepository.save(RolePermission.create(role.getId(), permissionId, now));
    }
}
