package com.jeepclub.backend.authorization.api.contract;

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
class AuthorizationOpenApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRegistration userRegistration;
    @Autowired private UserQuery userQuery;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private RolePermissionRepository rolePermissionRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private Clock clock;

    @Test
    void openApiDescribesAuthorizationResponsesPrecisely() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/authorization/me']['get']['responses']['401']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/authorization/permissions']['get']['responses']['200']['content']['application/json']['schema']['type']")
                        .value("array"))
                .andExpect(jsonPath("$['paths']['/authorization/permissions']['get']['responses']['200']['content']['application/json']['schema']['items']['$ref']")
                        .value("#/components/schemas/PermissionResponseDTO"))
                .andExpect(jsonPath("$['paths']['/authorization/roles']['get']['responses']['200']['content']['application/json']['schema']['type']")
                        .value("array"))
                .andExpect(jsonPath("$['paths']['/authorization/roles']['get']['responses']['200']['content']['application/json']['schema']['items']['$ref']")
                        .value("#/components/schemas/RoleResponseDTO"))
                .andExpect(jsonPath("$['paths']['/authorization/roles/{roleId}/permissions']['get']['responses']['200']['content']['application/json']['schema']['type']")
                        .value("array"))
                .andExpect(jsonPath("$['paths']['/authorization/roles/{roleId}/permissions']['get']['responses']['200']['content']['application/json']['schema']['items']['$ref']")
                        .value("#/components/schemas/PermissionResponseDTO"))
                .andExpect(jsonPath("$['paths']['/authorization/users/{userId}/roles']['get']['responses']['200']['content']['application/json']['schema']['type']")
                        .value("array"))
                .andExpect(jsonPath("$['paths']['/authorization/users/{userId}/roles']['get']['responses']['200']['content']['application/json']['schema']['items']['$ref']")
                        .value("#/components/schemas/RoleResponseDTO"))
                .andExpect(jsonPath("$['paths']['/authorization/roles']['post']['x-required-permissions'][0]")
                        .value("AUTHORIZATION_ROLE_CREATE"))
                .andExpect(jsonPath("$['paths']['/authorization/roles']['post']['responses']['401']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/authorization/roles']['post']['responses']['403']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/authorization/roles/{roleId}']['get']['responses']['500']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/authorization/roles/{roleId}']['put']['responses']['400']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$['paths']['/authorization/users/{userId}/roles']['put']['responses']['500']['content']['application/problem+json']['schema']['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"));
    }

    @Test
    @Transactional
    void positivePathVariablesAndAuthorizationExceptionsCurrentlyUseInternalServerError() throws Exception {
        Instant now = Instant.now(clock);
        String cpf = "34790621854";
        UserAuthenticationTokens tokens = userRegistration.registerAndAuthenticate(
                new UserRegistrationData(
                        "Authorization Validation", null, "authorization-validation@example.com", cpf,
                        null, null, null, now
                ),
                "security-password"
        );
        Long userId = userQuery.findByCpf(cpf).orElseThrow().id();
        Role role = roleRepository.save(Role.create("authorization-validation", "Test role", now));
        userRoleRepository.save(UserRole.create(userId, role.getId(), now));
        grant(role, PermissionCode.AUTHORIZATION_ROLE_READ, now);

        String bearer = "Bearer " + tokens.accessToken();

        mockMvc.perform(get("/authorization/roles/{roleId}", 0)
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));

        mockMvc.perform(get("/authorization/roles/{roleId}", 999999L)
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));
    }

    private void grant(Role role, PermissionCode code, Instant now) {
        Long permissionId = permissionRepository.findByCode(code).orElseThrow().getId();
        rolePermissionRepository.save(RolePermission.create(role.getId(), permissionId, now));
    }
}
