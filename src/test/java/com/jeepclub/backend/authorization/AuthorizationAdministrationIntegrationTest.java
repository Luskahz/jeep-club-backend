package com.jeepclub.backend.authorization;

import com.jeepclub.backend.BackendApplication;
import com.jeepclub.backend.iam.identity.api.module.*;
import com.jeepclub.backend.iam.authorization.api.module.RolePresentationQuery;
import com.jeepclub.backend.iam.authorization.core.application.service.role.AdminRoleService;
import com.jeepclub.backend.iam.authorization.core.application.service.userrole.AdminUserRoleService;
import com.jeepclub.backend.iam.authorization.core.application.service.rolepermission.AdminRolePermissionService;
import com.jeepclub.backend.iam.authorization.core.application.service.bootstrap.RootRoleBootstrapService;
import com.jeepclub.backend.iam.authorization.core.application.exception.role.RoleNotFoundException;
import com.jeepclub.backend.iam.authorization.core.domain.model.*;
import com.jeepclub.backend.iam.authorization.core.repository.*;
import com.jeepclub.backend.shared.authorization.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real JWT/session validation, Method Security, public Identity provisioning and Authorization persistence. */
@SpringBootTest(classes=BackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthorizationAdministrationIntegrationTest {
    static final AtomicLong IDS = new AtomicLong(83000000000L);
    @Autowired UserRegistration registration;
    @Autowired UserQuery identities;
    @Autowired UserIdentityLock identityLock;
    @Autowired AdminRoleService roles;
    @Autowired AdminUserRoleService users;
    @Autowired AdminRolePermissionService rolePermissions;
    @Autowired RoleRepository roleRepository;
    @Autowired UserRoleRepository bindings;
    @Autowired PermissionRepository permissions;
    @Autowired RolePermissionRepository permissionBindings;
    @Autowired RolePresentationQuery presentation;
    @Autowired RootRoleBootstrapService roots;
    @Autowired MockMvc mvc;
    @Autowired Clock clock;
    Long actor, target;
    Role actorRole;
    String token;

    @BeforeEach void provisionThroughPublicIdentityContract() {
        String cpf=Long.toString(IDS.incrementAndGet());
        token=registration.registerAndAuthenticate(new UserRegistrationData("Synthetic authorization actor",null,
                "actor-"+cpf+"@example.com",cpf,null,null,null,clock.instant()),"synthetic-password-123").accessToken();
        actor=identities.findByCpf(cpf).orElseThrow().id();
        target=registration.createWithPermanentCredential(new UserRegistrationData("Synthetic target",null,null,
                Long.toString(IDS.incrementAndGet()),null,null,null,clock.instant()),"synthetic-password-123");
        actorRole=roles.createRole("Actor "+UUID.randomUUID(),null).role();
        users.assignRoleToUser(actor,actorRole.getId());
    }
    Long permission(PermissionCode code) { return permissions.findByCode(code).orElseThrow().getId(); }
    void grant(PermissionCode... codes) {
        for(var code:codes)rolePermissions.assignPermissionToRole(actorRole.getId(),permission(code));
    }
    void problem(org.springframework.test.web.servlet.ResultActions response,int status,String code) throws Exception {
        response.andExpect(status().is(status)).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status)).andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.detail").isNotEmpty()).andExpect(jsonPath("$.stackTrace").doesNotExist());
    }
    @Test void anonymousAndAuthenticatedWithoutAuthorityCannotChangeBindings() throws Exception {
        problem(mvc.perform(post("/authorization/users/{user}/roles/{role}",target,actorRole.getId())),401,"AUTHENTICATION_REQUIRED");
        problem(mvc.perform(post("/authorization/users/{user}/roles/{role}",target,actorRole.getId()).header("Authorization","Bearer "+token)),403,"ACCESS_DENIED");
        problem(mvc.perform(delete("/authorization/users/{user}/roles/{role}",target,actorRole.getId()).header("Authorization","Bearer "+token)),403,"ACCESS_DENIED");
        problem(mvc.perform(put("/authorization/users/{user}/roles",target).header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"roleIds\":[]}")),403,"ACCESS_DENIED");
        problem(mvc.perform(post("/authorization/roles/{role}/permissions/{permission}",actorRole.getId(),permission(PermissionCode.IDENTITY_USER_READ))
                .header("Authorization","Bearer "+token)),403,"ACCESS_DENIED");
        assertThat(bindings.findRolesByUserId(target)).isEmpty();
    }
    @ParameterizedTest @EnumSource(value=PermissionCode.class,names={"AUTHORIZATION_USER_ROLE_ASSIGN","AUTHORIZATION_USER_ROLE_REVOKE"})
    void replacementRequiresBothAuthoritiesAndPreservesTheExistingBinding(PermissionCode only) throws Exception {
        grant(only);
        var retained=roles.createRole("Retained "+UUID.randomUUID(),null).role();
        users.assignRoleToUser(target,retained.getId());
        problem(mvc.perform(put("/authorization/users/{user}/roles",target).header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"roleIds\":[]}")),403,"ACCESS_DENIED");
        assertThat(bindings.findRolesByUserId(target)).extracting(Role::getId).containsExactly(retained.getId());
    }
    @Test void rootEscalationIsRejectedForAssignReplaceAndRevokeWithNoBindingChange() throws Exception {
        grant(PermissionCode.AUTHORIZATION_USER_ROLE_ASSIGN,PermissionCode.AUTHORIZATION_USER_ROLE_REVOKE);
        Long root=roots.ensureRootRole();
        problem(mvc.perform(post("/authorization/users/{user}/roles/{role}",target,root)
                .header("Authorization","Bearer "+token)),409,"ROOT_ROLE_CANNOT_BE_MANAGED_MANUALLY");
        problem(mvc.perform(put("/authorization/users/{user}/roles",target).header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"roleIds\":["+root+"]}")),409,"ROOT_ROLE_CANNOT_BE_MANAGED_MANUALLY");
        problem(mvc.perform(delete("/authorization/users/{user}/roles/{role}",target,root)
                .header("Authorization","Bearer "+token)),409,"ROOT_ROLE_CANNOT_BE_MANAGED_MANUALLY");
        assertThat(bindings.findRolesByUserId(target)).isEmpty();
    }
    @Test void unknownUserUnknownRoleInactiveRoleAndDuplicateAreControlledProblems() throws Exception {
        grant(PermissionCode.AUTHORIZATION_USER_ROLE_ASSIGN);
        problem(mvc.perform(post("/authorization/users/{user}/roles/{role}",Long.MAX_VALUE,actorRole.getId())
                .header("Authorization","Bearer "+token)),404,"USER_NOT_FOUND");
        problem(mvc.perform(post("/authorization/users/{user}/roles/{role}",target,Long.MAX_VALUE)
                .header("Authorization","Bearer "+token)),404,"ROLE_NOT_FOUND");
        var inactive=roles.createRole("Inactive "+UUID.randomUUID(),null).role();
        roles.deactivateRole(inactive.getId());
        problem(mvc.perform(post("/authorization/users/{user}/roles/{role}",target,inactive.getId())
                .header("Authorization","Bearer "+token)),409,"INACTIVE_ROLE_CANNOT_BE_USED");
        mvc.perform(post("/authorization/users/{user}/roles/{role}",target,actorRole.getId())
                .header("Authorization","Bearer "+token)).andExpect(status().isCreated());
        problem(mvc.perform(post("/authorization/users/{user}/roles/{role}",target,actorRole.getId())
                .header("Authorization","Bearer "+token)),409,"USER_ROLE_ALREADY_EXISTS");
        assertThat(bindings.findRolesByUserId(target)).extracting(Role::getId).containsExactly(actorRole.getId());
    }
    @Test void currentTokenImmediatelyLosesRemovedPermissionInactiveRoleAndRevokedBinding() throws Exception {
        grant(PermissionCode.AUTHORIZATION_ROLE_READ);
        mvc.perform(get("/authorization/roles").header("Authorization","Bearer "+token)).andExpect(status().isOk());
        rolePermissions.removePermissionFromRole(actorRole.getId(),permission(PermissionCode.AUTHORIZATION_ROLE_READ));
        problem(mvc.perform(get("/authorization/roles").header("Authorization","Bearer "+token)),403,"ACCESS_DENIED");
        mvc.perform(get("/authorization/me").header("Authorization","Bearer "+token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.authorities").isEmpty());
        grant(PermissionCode.AUTHORIZATION_ROLE_READ);
        roles.deactivateRole(actorRole.getId());
        problem(mvc.perform(get("/authorization/roles").header("Authorization","Bearer "+token)),403,"ACCESS_DENIED");
        roles.activateRole(actorRole.getId());
        mvc.perform(get("/authorization/roles").header("Authorization","Bearer "+token)).andExpect(status().isOk());
        users.revokeRoleFromUser(actor,actorRole.getId());
        problem(mvc.perform(get("/authorization/roles").header("Authorization","Bearer "+token)),403,"ACCESS_DENIED");
    }
    @Test void invalidReplacementPreservesAllCurrentBindings() {
        users.assignRoleToUser(target,actorRole.getId());
        var replacement=roles.createRole("Replacement "+UUID.randomUUID(),null).role();
        assertThatThrownBy(() -> users.replaceUserRoles(target,List.of(replacement.getId(),Long.MAX_VALUE)))
                .isInstanceOf(RoleNotFoundException.class);
        assertThat(bindings.findRolesByUserId(target)).extracting(Role::getId).containsExactly(actorRole.getId());
    }
    @Test void presentationCombinesCurrentDeletedAndUnknownIdsThroughRealQueries() {
        var deleted=roles.createRole("Deleted "+UUID.randomUUID(),null).role();
        roles.deleteRole(deleted.getId(),actor);
        assertThat(presentation.findByIds(List.of(actorRole.getId(),deleted.getId(),Long.MAX_VALUE)))
                .containsExactlyInAnyOrder(new RolePresentationQuery.Details(actorRole.getId(),actorRole.getName()),
                        new RolePresentationQuery.Details(deleted.getId(),deleted.getName()));
        assertThat(presentation.findByIds(List.of(actorRole.getId())))
                .containsExactly(new RolePresentationQuery.Details(actorRole.getId(),actorRole.getName()));
        assertThat(presentation.findByIds(List.of(Long.MAX_VALUE))).isEmpty();
    }

    @Test void permissionBindingFailuresKeepTheirRfcContractsAndDoNotChangeTheCatalog() throws Exception {
        grant(PermissionCode.AUTHORIZATION_PERMISSION_ASSIGN,PermissionCode.AUTHORIZATION_PERMISSION_REVOKE);
        var targetRole=roles.createRole("Permission target "+UUID.randomUUID(),null).role();
        Long permission=permission(PermissionCode.IDENTITY_USER_READ);
        String route="/authorization/roles/{role}/permissions/{permission}";
        problem(mvc.perform(delete(route,targetRole.getId(),permission).header("Authorization","Bearer "+token)),
                404,"ROLE_PERMISSION_NOT_FOUND");
        problem(mvc.perform(post(route,targetRole.getId(),Long.MAX_VALUE).header("Authorization","Bearer "+token)),
                404,"PERMISSION_NOT_FOUND");
        problem(mvc.perform(post(route,Long.MAX_VALUE,permission).header("Authorization","Bearer "+token)),
                404,"ROLE_NOT_FOUND");
        mvc.perform(post(route,targetRole.getId(),permission).header("Authorization","Bearer "+token)).andExpect(status().isCreated());
        problem(mvc.perform(post(route,targetRole.getId(),permission).header("Authorization","Bearer "+token)),
                409,"ROLE_PERMISSION_ALREADY_EXISTS");
        mvc.perform(delete(route,targetRole.getId(),permission).header("Authorization","Bearer "+token)).andExpect(status().isNoContent());
        assertThat(permissionBindings.findPermissionsByRoleId(targetRole.getId())).isEmpty();
        assertThat(permissions.findById(permission)).isPresent();
    }
}
