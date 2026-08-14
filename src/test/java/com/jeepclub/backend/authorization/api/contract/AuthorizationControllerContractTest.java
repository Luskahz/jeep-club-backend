package com.jeepclub.backend.authorization.api.contract;

import com.jeepclub.backend.authorization.api.http.controller.admin.AdminPermissionController;
import com.jeepclub.backend.authorization.api.http.controller.admin.AdminRoleController;
import com.jeepclub.backend.authorization.api.http.controller.admin.AdminRolePermissionController;
import com.jeepclub.backend.authorization.api.http.controller.admin.AdminUserRoleController;
import com.jeepclub.backend.authorization.core.application.result.PermissionResult;
import com.jeepclub.backend.authorization.core.application.result.PermissionsResult;
import com.jeepclub.backend.authorization.core.application.result.RoleResult;
import com.jeepclub.backend.authorization.core.application.result.RolesResult;
import com.jeepclub.backend.authorization.core.application.service.permission.AdminPermissionService;
import com.jeepclub.backend.authorization.core.application.service.role.AdminRoleService;
import com.jeepclub.backend.authorization.core.application.service.rolepermission.AdminRolePermissionService;
import com.jeepclub.backend.authorization.core.application.service.userrole.AdminUserRoleService;
import com.jeepclub.backend.authorization.core.domain.enums.RoleStatus;
import com.jeepclub.backend.authorization.core.domain.model.Permission;
import com.jeepclub.backend.authorization.core.domain.model.Role;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.shared.authorization.ModuleCode;
import com.jeepclub.backend.shared.authorization.PermissionCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthorizationControllerContractTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Instant now = Instant.parse("2026-08-13T12:00:00Z");
        Permission permission = Permission.reconstitute(
                1L,
                PermissionCode.AUTHORIZATION_ROLE_READ,
                "Permite consultar papéis de acesso",
                ModuleCode.AUTHORIZATION,
                now,
                now
        );
        Role role = Role.reconstitute(
                1L,
                "ADMIN",
                "Administrador",
                RoleStatus.ACTIVE,
                now,
                now,
                null
        );

        AdminPermissionService permissionService = mock(AdminPermissionService.class);
        when(permissionService.findAllPermissions()).thenReturn(new PermissionsResult(List.of(permission)));
        when(permissionService.findPermissionById(anyLong())).thenReturn(new PermissionResult(permission));
        when(permissionService.findPermissionByCode(anyString())).thenReturn(new PermissionResult(permission));

        AdminRoleService roleService = mock(AdminRoleService.class);
        when(roleService.createRole(anyString(), nullable(String.class)))
                .thenReturn(new RoleResult(role));
        when(roleService.findAllRoles()).thenReturn(new RolesResult(List.of(role)));
        when(roleService.findRoleById(anyLong())).thenReturn(new RoleResult(role));
        when(roleService.updateRole(
                anyLong(),
                anyString(),
                nullable(String.class)
        )).thenReturn(new RoleResult(role));
        when(roleService.deactivateRole(anyLong())).thenReturn(new RoleResult(role));
        when(roleService.activateRole(anyLong())).thenReturn(new RoleResult(role));

        AdminRolePermissionService rolePermissionService = mock(AdminRolePermissionService.class);
        when(rolePermissionService.findPermissionsByRoleId(anyLong()))
                .thenReturn(new PermissionsResult(List.of(permission)));

        AdminUserRoleService userRoleService = mock(AdminUserRoleService.class);
        when(userRoleService.findRolesByUserId(anyLong())).thenReturn(new RolesResult(List.of(role)));

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AdminPermissionController(permissionService),
                        new AdminRoleController(roleService),
                        new AdminRolePermissionController(rolePermissionService),
                        new AdminUserRoleController(userRoleService)
                )
                .setValidator(validator)
                .build();
    }

    @Test
    void keepsPublishedRoutesVerbsAndSecurityRules() {
        assertEndpoint(
                AdminPermissionController.class,
                "findAllPermissions",
                "/authorization/permissions",
                "",
                RequestMethod.GET,
                "hasAuthority('AUTHORIZATION_PERMISSION_READ')",
                "AUTHORIZATION_PERMISSION_READ"
        );
        assertEndpoint(
                AdminPermissionController.class,
                "findPermissionById",
                "/authorization/permissions",
                "/{permissionId}",
                RequestMethod.GET,
                "hasAuthority('AUTHORIZATION_PERMISSION_READ')",
                "AUTHORIZATION_PERMISSION_READ"
        );
        assertEndpoint(
                AdminPermissionController.class,
                "findPermissionByCode",
                "/authorization/permissions",
                "/code/{permissionCode}",
                RequestMethod.GET,
                "hasAuthority('AUTHORIZATION_PERMISSION_READ')",
                "AUTHORIZATION_PERMISSION_READ"
        );

        assertEndpoint(AdminRoleController.class, "createRole", "/authorization/roles", "", RequestMethod.POST,
                "hasAuthority('AUTHORIZATION_ROLE_CREATE')", "AUTHORIZATION_ROLE_CREATE");
        assertEndpoint(AdminRoleController.class, "findAllRoles", "/authorization/roles", "", RequestMethod.GET,
                "hasAuthority('AUTHORIZATION_ROLE_READ')", "AUTHORIZATION_ROLE_READ");
        assertEndpoint(AdminRoleController.class, "findRoleById", "/authorization/roles", "/{roleId}",
                RequestMethod.GET, "hasAuthority('AUTHORIZATION_ROLE_READ')", "AUTHORIZATION_ROLE_READ");
        assertEndpoint(AdminRoleController.class, "updateRole", "/authorization/roles", "/{roleId}",
                RequestMethod.PUT, "hasAuthority('AUTHORIZATION_ROLE_UPDATE')", "AUTHORIZATION_ROLE_UPDATE");
        assertEndpoint(AdminRoleController.class, "deactivateRole", "/authorization/roles", "/{roleId}/deactivate",
                RequestMethod.PATCH, "hasAuthority('AUTHORIZATION_ROLE_DISABLE')", "AUTHORIZATION_ROLE_DISABLE");
        assertEndpoint(AdminRoleController.class, "activateRole", "/authorization/roles", "/{roleId}/activate",
                RequestMethod.PATCH, "hasAuthority('AUTHORIZATION_ROLE_ENABLE')", "AUTHORIZATION_ROLE_ENABLE");
        assertEndpoint(AdminRoleController.class, "deleteRole", "/authorization/roles", "/{roleId}",
                RequestMethod.DELETE, "hasAuthority('AUTHORIZATION_ROLE_DELETE')", "AUTHORIZATION_ROLE_DELETE");

        assertEndpoint(AdminRolePermissionController.class, "findPermissionsByRoleId",
                "/authorization/roles/{roleId}/permissions", "", RequestMethod.GET,
                "hasAuthority('AUTHORIZATION_PERMISSION_READ')", "AUTHORIZATION_PERMISSION_READ");
        assertEndpoint(AdminRolePermissionController.class, "assignPermissionToRole",
                "/authorization/roles/{roleId}/permissions", "/{permissionId}", RequestMethod.POST,
                "hasAuthority('AUTHORIZATION_PERMISSION_ASSIGN')", "AUTHORIZATION_PERMISSION_ASSIGN");
        assertEndpoint(AdminRolePermissionController.class, "removePermissionFromRole",
                "/authorization/roles/{roleId}/permissions", "/{permissionId}", RequestMethod.DELETE,
                "hasAuthority('AUTHORIZATION_PERMISSION_REVOKE')", "AUTHORIZATION_PERMISSION_REVOKE");

        assertEndpoint(AdminUserRoleController.class, "findRolesByUser", "/authorization/users", "/{userId}/roles",
                RequestMethod.GET, "hasAuthority('AUTHORIZATION_USER_ROLE_READ')", "AUTHORIZATION_USER_ROLE_READ");
        assertEndpoint(AdminUserRoleController.class, "replaceUserRoles", "/authorization/users", "/{userId}/roles",
                RequestMethod.PUT,
                "hasAuthority('AUTHORIZATION_USER_ROLE_ASSIGN') and hasAuthority('AUTHORIZATION_USER_ROLE_REVOKE')",
                "AUTHORIZATION_USER_ROLE_ASSIGN", "AUTHORIZATION_USER_ROLE_REVOKE");
        assertEndpoint(AdminUserRoleController.class, "assignRoleToUser", "/authorization/users",
                "/{userId}/roles/{roleId}", RequestMethod.POST,
                "hasAuthority('AUTHORIZATION_USER_ROLE_ASSIGN')", "AUTHORIZATION_USER_ROLE_ASSIGN");
        assertEndpoint(AdminUserRoleController.class, "revokeRoleFromUser", "/authorization/users",
                "/{userId}/roles/{roleId}", RequestMethod.DELETE,
                "hasAuthority('AUTHORIZATION_USER_ROLE_REVOKE')", "AUTHORIZATION_USER_ROLE_REVOKE");
    }

    @Test
    void keepsPublishedSuccessStatuses() throws Exception {
        String roleBody = """
                {
                  "name": "ADMIN",
                  "description": "Administrador"
                }
                """;

        mockMvc.perform(get("/authorization/permissions")).andExpect(status().isOk());
        mockMvc.perform(get("/authorization/permissions/{permissionId}", 1L)).andExpect(status().isOk());
        mockMvc.perform(get("/authorization/permissions/code/{permissionCode}", "AUTHORIZATION_ROLE_READ"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/authorization/roles").contentType(MediaType.APPLICATION_JSON).content(roleBody))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/authorization/roles")).andExpect(status().isOk());
        mockMvc.perform(get("/authorization/roles/{roleId}", 1L)).andExpect(status().isOk());
        mockMvc.perform(put("/authorization/roles/{roleId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roleBody))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/authorization/roles/{roleId}/deactivate", 1L)).andExpect(status().isOk());
        mockMvc.perform(patch("/authorization/roles/{roleId}/activate", 1L)).andExpect(status().isOk());
        mockMvc.perform(delete("/authorization/roles/{roleId}", 1L)).andExpect(status().isNoContent());

        mockMvc.perform(get("/authorization/roles/{roleId}/permissions", 1L)).andExpect(status().isOk());
        mockMvc.perform(post("/authorization/roles/{roleId}/permissions/{permissionId}", 1L, 1L))
                .andExpect(status().isCreated());
        mockMvc.perform(delete("/authorization/roles/{roleId}/permissions/{permissionId}", 1L, 1L))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/authorization/users/{userId}/roles", 1L)).andExpect(status().isOk());
        mockMvc.perform(put("/authorization/users/{userId}/roles", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[]}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/authorization/users/{userId}/roles/{roleId}", 1L, 1L))
                .andExpect(status().isCreated());
        mockMvc.perform(delete("/authorization/users/{userId}/roles/{roleId}", 1L, 1L))
                .andExpect(status().isNoContent());
    }

    private static void assertEndpoint(
            Class<?> controllerType,
            String methodName,
            String expectedControllerPath,
            String expectedMethodPath,
            RequestMethod expectedVerb,
            String expectedAuthorization,
            String... expectedPermissions
    ) {
        RequestMapping controllerMapping = controllerType.getAnnotation(RequestMapping.class);
        assertThat(controllerMapping.value()).containsExactly(expectedControllerPath);

        Method method = Arrays.stream(controllerType.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow();

        RequestMapping methodMapping = findMergedAnnotation(method, RequestMapping.class);
        assertThat(methodMapping).isNotNull();
        if (expectedMethodPath.isEmpty()) {
            assertThat(methodMapping.value()).isEmpty();
        } else {
            assertThat(methodMapping.value()).containsExactly(expectedMethodPath);
        }
        assertThat(methodMapping.method()).containsExactly(expectedVerb);

        PreAuthorize preAuthorize = findMergedAnnotation(method, PreAuthorize.class);
        if (preAuthorize == null) {
            preAuthorize = findMergedAnnotation(controllerType, PreAuthorize.class);
        }
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(expectedAuthorization);

        RequiredPermission requiredPermission = findMergedAnnotation(method, RequiredPermission.class);
        if (requiredPermission == null) {
            requiredPermission = findMergedAnnotation(controllerType, RequiredPermission.class);
        }
        assertThat(requiredPermission).isNotNull();
        assertThat(requiredPermission.value()).containsExactly(expectedPermissions);
    }
}
