package com.jeepclub.backend.lifecycle;

import com.jeepclub.backend.iam.identity.api.module.*;
import com.jeepclub.backend.iam.identity.core.domain.model.User;
import com.jeepclub.backend.iam.identity.core.repository.UserRepository;
import com.jeepclub.backend.iam.identity.core.application.service.history.UserDeletionHistoryService;
import com.jeepclub.backend.iam.authentication.core.domain.model.AuthenticationAccount;
import com.jeepclub.backend.iam.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.iam.authentication.core.repository.SessionRepository;
import com.jeepclub.backend.iam.authentication.core.domain.model.Session;
import com.jeepclub.backend.iam.authorization.core.application.service.role.AdminRoleService;
import com.jeepclub.backend.iam.authorization.core.application.service.userrole.AdminUserRoleService;
import com.jeepclub.backend.iam.authorization.core.application.service.history.RoleDeletionHistoryService;
import com.jeepclub.backend.iam.authorization.core.repository.RoleRepository;
import com.jeepclub.backend.iam.authorization.api.module.RolePresentationQuery;
import com.jeepclub.backend.iam.authorization.core.domain.enums.RoleStatus;
import com.jeepclub.backend.dependents.core.application.service.dependent.DependentService;
import com.jeepclub.backend.dependents.core.domain.enums.*;
import com.jeepclub.backend.tools.core.application.service.tool.AdminToolService;
import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import com.jeepclub.backend.vehicles.core.application.service.vehicle.AdminVehicleService;
import com.jeepclub.backend.vehicles.core.domain.enums.*;
import com.jeepclub.backend.billing.core.application.service.chargedefinition.AdminChargeDefinitionService;
import com.jeepclub.backend.billing.core.application.service.chargeassignment.AdminChargeAssignmentService;
import com.jeepclub.backend.billing.core.application.service.history.ChargeDefinitionDeletionHistoryService;
import com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType;
import com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy;
import com.jeepclub.backend.platform.logging.SystemLogService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

/** Real application services and persistence; inherited by the migrated MySQL suite. */
@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@ActiveProfiles("test")
class PrincipalLifecycleIntegrationTest {
    static final Instant CREATED = Instant.parse("2026-10-01T00:00:00Z");
    static final AtomicLong IDS = new AtomicLong(76000000000L);
    @Autowired UserRepository users;
    @Autowired UserAdministration identity;
    @Autowired AuthenticationAccountRepository accounts;
    @Autowired SessionRepository sessions;
    @Autowired UserDeletionHistoryService userHistory;
    @Autowired AdminRoleService roles;
    @Autowired RoleRepository roleRepository;
    @Autowired AdminUserRoleService userRoles;
    @Autowired RoleDeletionHistoryService roleHistory;
    @Autowired RolePresentationQuery roleNames;
    @Autowired DependentService dependents;
    @Autowired AdminToolService tools;
    @Autowired AdminVehicleService vehicles;
    @Autowired AdminChargeDefinitionService definitions;
    @Autowired AdminChargeAssignmentService assignments;
    @Autowired ChargeDefinitionDeletionHistoryService definitionHistory;
    @Autowired com.jeepclub.backend.iam.authorization.core.application.service.bootstrap.RootRoleBootstrapService rootRoles;
    @Autowired com.jeepclub.backend.iam.authorization.core.application.service.bootstrap.UserRoleAssignmentService bootstrapRoles;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate transactions;
    @MockitoSpyBean SystemLogService audit;
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.jeepclub.backend.platform.security.jwt.JwtTokenParser parser;
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired com.jeepclub.backend.iam.authorization.core.repository.PermissionRepository permissions;
    @Autowired com.jeepclub.backend.iam.authorization.core.application.service.rolepermission.AdminRolePermissionService rolePermissions;

    String access(Long id) {
        var session=tx(() -> sessions.save(Session.create(id,Duration.ofDays(365),CREATED)));
        String token="synthetic-lifecycle-"+UUID.randomUUID();
        when(parser.parseAndValidate(token)).thenReturn(new com.jeepclub.backend.platform.security.jwt.JwtAuthenticatedUser(
                id,session.getId(),"Synthetic lifecycle actor",Instant.now().plusSeconds(3600)));
        return token;
    }
    Long grant(Long id, com.jeepclub.backend.shared.authorization.PermissionCode... codes) {
        var role=roles.createRole("HTTP "+UUID.randomUUID(),null).role();
        for(var code:codes) {
            var permission=permissions.findByCode(code).orElseGet(() -> tx(() -> permissions.save(
                    com.jeepclub.backend.iam.authorization.core.domain.model.Permission.fromDefinition(
                            Arrays.stream(com.jeepclub.backend.shared.authorization.PermissionDefinition.values())
                                    .filter(definition -> definition.getCode()==code).findFirst().orElseThrow(),CREATED))));
            rolePermissions.assignPermissionToRole(role.getId(),permission.getId());
        }
        userRoles.assignRoleToUser(id,role.getId());
        return role.getId();
    }

    @AfterEach void resetAudit() { reset(audit); }
    <T> T tx(Supplier<T> action) { return transactions.execute(status -> action.get()); }
    Long user(boolean pending) {
        return tx(() -> {
            var user = users.create(User.create("Synthetic lifecycle", null, null, String.valueOf(IDS.incrementAndGet()), null, null, null, CREATED));
            accounts.create(pending ? AuthenticationAccount.createPendingFirstAccess(user.getId(), "synthetic-unusable-hash", CREATED)
                    : AuthenticationAccount.create(user.getId(), "synthetic-unusable-hash", CREATED));
            return user.getId();
        });
    }
    Long definition() {
        return definitions.create("Synthetic "+UUID.randomUUID(),"retained config",new BigDecimal("125.50"),
                ChargeRecurrenceType.ONE_TIME,true,PaymentAcceptancePolicy.AFTER_DUE_DATE,null).id();
    }

    @Test void userArchivalRevokesSessionsAndRequiresUnarchiveThenEnable() {
        Long id=user(false);
        var session=tx(() -> sessions.save(Session.create(id,Duration.ofDays(365),CREATED)));
        identity.archive(id,id,CREATED.plusSeconds(60));
        assertThat(users.findById(id).orElseThrow().getStatus()).isEqualTo(UserStatus.ARCHIVED);
        assertThat(accounts.findByIdentityId(id).orElseThrow().isAccessDisabled()).isTrue();
        assertThat(sessions.findById(session.getId()).orElseThrow().isActive(CREATED.plusSeconds(61))).isFalse();
        assertThatThrownBy(() -> identity.enable(id,CREATED.plusSeconds(90))).isInstanceOf(RuntimeException.class);
        identity.unarchive(id,id,CREATED.plusSeconds(120));
        assertThat(users.findById(id).orElseThrow().getStatus()).isEqualTo(UserStatus.INACTIVE);
        assertThat(accounts.findByIdentityId(id).orElseThrow().isAccessDisabled()).isTrue();
        identity.enable(id,CREATED.plusSeconds(180));
        assertThat(users.findById(id).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(sessions.findById(session.getId()).orElseThrow().isActive(CREATED.plusSeconds(181))).isFalse();
    }

    @Test void pendingUserDeletionArchivesAuthorizationBindingsAndFreesBusinessIdentifiers() {
        Long id=user(true); String cpf=users.findById(id).orElseThrow().getCpf();
        var role=roles.createRole("Pending role "+UUID.randomUUID(),null).role();
        userRoles.assignRoleToUser(id,role.getId());
        identity.delete(id,id,CREATED.plusSeconds(60));
        assertThat(users.findById(id)).isEmpty();
        assertThat(accounts.findByIdentityId(id)).isEmpty();
        assertThat(userHistory.find(id,id).previousStatus()).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM authorization_user_roles WHERE user_id=?",Long.class,id)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM authorization_user_role_removal_history WHERE user_id=?",Long.class,id)).isEqualTo(1);
        assertThat(roleRepository.findById(role.getId())).isPresent();
        assertThatCode(() -> tx(() -> users.create(User.create("Replacement",null,null,cpf,null,null,null,CREATED)))).doesNotThrowAnyException();
    }

    @Test void completedCredentialsAndAnySessionFactBlockPhysicalDeletion() {
        Long completed=user(false);
        assertThatThrownBy(() -> identity.delete(completed,completed,CREATED.plusSeconds(60)))
                .isInstanceOf(com.jeepclub.backend.iam.identity.api.module.exception.UserDeletionConflictException.class);
        Long pending=user(true);
        tx(() -> sessions.save(Session.create(pending,Duration.ofHours(1),CREATED)));
        assertThatThrownBy(() -> identity.delete(pending,pending,CREATED.plusSeconds(60)))
                .isInstanceOf(com.jeepclub.backend.iam.identity.api.module.exception.UserDeletionConflictException.class);
        assertThat(users.findById(completed)).isPresent();
        assertThat(users.findById(pending)).isPresent();
    }

    @Test void registeredMemberIsNeverPhysicallyDeleted() {
        Long id=user(true);
        jdbc.update("INSERT INTO membership_members(identity_user_id,membership_number,created_at,updated_at,version) VALUES(?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0)",id,"91"+id);
        assertThatThrownBy(() -> identity.delete(id,id,CREATED.plusSeconds(60)))
                .isInstanceOf(com.jeepclub.backend.iam.identity.api.module.exception.UserDeletionConflictException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM membership_members WHERE identity_user_id=?",Long.class,id)).isEqualTo(1);
        assertThat(users.findById(id)).isPresent();
    }

    @Test void financialAssignmentPreventsUserAndDefinitionDeletionWithoutDestroyingFacts() {
        Long id=user(true), definition=definition();
        assignments.assignToUser(definition,id);
        assertThatThrownBy(() -> identity.delete(id,id,CREATED.plusSeconds(60))).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> definitions.delete(definition,id)).isInstanceOf(RuntimeException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM billing_user_charge_assignments WHERE user_id=?",Long.class,id)).isEqualTo(1);
        assertThat(users.findById(id)).isPresent();
        assertThat(definitions.findById(definition).id()).isEqualTo(definition);
    }

    @Test void auditFailureRollsBackPendingUserAccountAndBindingDeletion() {
        Long id=user(true); var role=roles.createRole("Rollback "+UUID.randomUUID(),null).role();
        userRoles.assignRoleToUser(id,role.getId());
        tx(() -> { doThrow(new IllegalStateException("synthetic audit outage")).when(audit).recordInTransaction(any()); return null; });
        assertThatThrownBy(() -> identity.delete(id,id,CREATED.plusSeconds(60))).hasMessage("synthetic audit outage");
        assertThat(users.findById(id)).isPresent(); assertThat(accounts.findByIdentityId(id)).isPresent();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM authorization_user_roles WHERE user_id=?",Long.class,id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM identity_user_deletion_history WHERE original_id=?",Long.class,id)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM authorization_user_role_removal_history WHERE user_id=?",Long.class,id)).isZero();
    }

    @Test void dependentArchiveReadRestoreAndPhysicalHistoryPreserveOwner() {
        Long owner=user(false);
        var d=dependents.create("Synthetic dependent",String.valueOf(IDS.incrementAndGet()),LocalDate.of(2010,1,1),RelationshipType.CHILD,null,owner);
        dependents.archive(d.id(),owner,owner);
        var archived=dependents.findById(d.id(),owner);
        assertThat(archived.status()).isEqualTo(DependentStatus.ARCHIVED);
        assertThatThrownBy(() -> dependents.enable(d.id(),owner)).isInstanceOf(RuntimeException.class);
        dependents.unarchive(d.id(),owner,owner);
        assertThat(dependents.findById(d.id(),owner).status()).isEqualTo(DependentStatus.INACTIVE);
        dependents.enable(d.id(),owner);
        dependents.archive(d.id(),owner,owner);
        dependents.delete(d.id(),owner);
        assertThat(jdbc.queryForObject("SELECT status FROM dependents_dependent_history WHERE dependent_id=?",String.class,d.id())).isEqualTo("ARCHIVED");
        assertThat(jdbc.queryForObject("SELECT user_id FROM dependents_dependent_history WHERE dependent_id=?",Long.class,d.id())).isEqualTo(owner);
    }

    @Test void toolArchiveRemainsReadableAndDeletionPreservesHistoricalState() {
        Long owner=user(false); var tool=tools.createToolForUser(owner,"Synthetic tool",null);
        tools.archiveTool(tool.getId(),owner);
        assertThat(tools.getToolDetails(tool.getId()).getStatus()).isEqualTo(ToolStatus.ARCHIVED);
        assertThatThrownBy(() -> tools.updateTool(tool.getId(),"Changed",null)).isInstanceOf(RuntimeException.class);
        tools.unarchiveTool(tool.getId(),owner);
        assertThat(tools.getToolDetails(tool.getId()).getStatus()).isEqualTo(ToolStatus.INACTIVE);
        tools.activateTool(tool.getId()); tools.archiveTool(tool.getId(),owner); tools.deleteTool(tool.getId(),owner);
        assertThat(jdbc.queryForObject("SELECT status FROM tools_tool_history WHERE tool_id=?",String.class,tool.getId())).isEqualTo("ARCHIVED");
    }

    @Test void vehicleArchiveFilterAndDeletionPreserveHistoricalState() {
        Long owner=user(false); long unique=IDS.incrementAndGet();
        var vehicle=vehicles.createForOwner("Synthetic",null,"TST"+String.format("%04d",unique%10000),String.valueOf(unique),"Jeep","Test",2020,2020,"Blue",5,FuelType.DIESEL,2d,false,owner);
        vehicles.archive(vehicle.getId(),owner);
        assertThat(vehicles.findById(vehicle.getId()).getStatus()).isEqualTo(VehicleStatus.ARCHIVED);
        assertThat(vehicles.findAllWithOwners(VehicleStatus.ARCHIVED,org.springframework.data.domain.Pageable.unpaged()).getContent())
                .extracting(summary -> summary.vehicle().getId()).contains(vehicle.getId());
        vehicles.unarchive(vehicle.getId(),owner);
        assertThat(vehicles.findById(vehicle.getId()).getStatus()).isEqualTo(VehicleStatus.INACTIVE);
        vehicles.activate(vehicle.getId(),owner); vehicles.archive(vehicle.getId(),owner); vehicles.delete(vehicle.getId(),owner);
        assertThat(jdbc.queryForObject("SELECT status FROM vehicles_vehicle_history WHERE vehicle_id=?",String.class,vehicle.getId())).isEqualTo("ARCHIVED");
    }

    @Test void roleArchiveAndDeletePreserveNameForHistoricalPresentation() {
        var role=roles.createRole("Historical "+UUID.randomUUID(),"retained").role();
        roles.archiveRole(role.getId(),1L);
        assertThat(roles.findRoleById(role.getId()).role().getStatus()).isEqualTo(RoleStatus.ARCHIVED);
        roles.unarchiveRole(role.getId(),1L);
        assertThat(roles.findRoleById(role.getId()).role().getStatus()).isEqualTo(RoleStatus.INACTIVE);
        roles.activateRole(role.getId()); roles.deleteRole(role.getId(),1L);
        assertThat(roleRepository.findById(role.getId())).isEmpty();
        assertThat(roleHistory.find(role.getId(),1L).name()).isEqualTo(role.getName());
        assertThat(roleNames.findByIds(List.of(role.getId()))).extracting(RolePresentationQuery.Details::name).containsExactly(role.getName());
    }

    @Test void assignedRoleDeletionConflictsWithoutLosingUserBinding() {
        Long user=user(true); var role=roles.createRole("Assigned "+UUID.randomUUID(),null).role();
        userRoles.assignRoleToUser(user,role.getId());
        assertThatThrownBy(() -> roles.deleteRole(role.getId(),user)).isInstanceOf(RuntimeException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM authorization_user_roles WHERE user_id=? AND role_id=?",Long.class,user,role.getId())).isEqualTo(1);
        assertThat(roleRepository.findById(role.getId())).isPresent();
    }

    @Test void unusedRoleDeletionSnapshotsPermissionsWithoutDeletingTheCatalog() {
        var role=roles.createRole("Permission history "+UUID.randomUUID(),null).role();
        var permission=permissions.findByCode(com.jeepclub.backend.shared.authorization.PermissionCode.TOOLS_TOOL_READ).orElseThrow();
        rolePermissions.assignPermissionToRole(role.getId(),permission.getId());
        roles.deleteRole(role.getId(),1L);
        assertThat(roleHistory.find(role.getId(),1L).permissions())
                .extracting(com.jeepclub.backend.iam.authorization.core.repository.RoleDeletionHistoryQueryRepository.RoleDeletionHistoryPermission::permissionCode)
                .containsExactly("TOOLS_TOOL_READ");
        assertThat(permissions.findById(permission.getId())).isPresent();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM authorization_role_permissions WHERE role_id=?",Long.class,role.getId())).isZero();
    }

    @Test void clinicalDependentReferencePreventsDeletionAndRetainsBothRecords() {
        Long owner=user(false);
        var dependent=dependents.create("Clinical dependent",String.valueOf(IDS.incrementAndGet()),LocalDate.of(2010,1,1),RelationshipType.CHILD,null,owner);
        Long profile=tx(() -> {
            var p=new com.jeepclub.backend.health.infra.persistence.entity.MedicalProfileEntity();
            p.setOwnerType(com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType.DEPENDENT);
            p.setOwnerId(dependent.id()); p.setBloodType(com.jeepclub.backend.health.core.domain.enums.BloodType.UNKNOWN);
            p.setCreatedAt(CREATED); p.setUpdatedAt(CREATED); entityManager.persist(p); entityManager.flush(); return p.getId();
        });
        assertThatThrownBy(() -> dependents.delete(dependent.id(),owner))
                .isInstanceOf(com.jeepclub.backend.dependents.core.domain.exception.DependentLifecycleConflictException.class);
        assertThat(dependents.findById(dependent.id(),owner).id()).isEqualTo(dependent.id());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM medical_profiles WHERE id=?",Long.class,profile)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dependents_dependent_history WHERE dependent_id=?",Long.class,dependent.id())).isZero();
    }

    @Test void administrativeLifecycleCommandsRejectAnAuthenticatedUserWithoutTheirPermission() throws Exception {
        Long user=user(false); String token=access(user);
        for(String path:List.of("/identity/admin/users/77/archive","/identity/admin/users/77/unarchive",
                "/authorization/roles/77/archive","/authorization/roles/77/unarchive",
                "/users/77/dependents/88/archive","/admin/tools/77/archive","/vehicles/archive/admin/77")) {
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(path)
                    .header("Authorization","Bearer "+token)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
        }
        for(String path:List.of("/identity/admin/users/77","/billing/charge-definitions/77","/users/77/dependents/88")) {
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(path)
                    .header("Authorization","Bearer "+token)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(path))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
        }
    }

    @Test void openApiPublishesTheNewPermissionsAndDistinctHistoryResponseModels() throws Exception {
        var response=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/v3/api-docs"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();
        var document=new com.fasterxml.jackson.databind.ObjectMapper().readTree(response);
        for(var contract:List.of(
                List.of("/identity/admin/users/{userId}/archive","patch","IDENTITY_USER_ARCHIVE"),
                List.of("/identity/admin/users/{userId}","delete","IDENTITY_USER_DELETE"),
                List.of("/authorization/roles/{roleId}/archive","patch","AUTHORIZATION_ROLE_ARCHIVE"),
                List.of("/users/{userId}/dependents/{id}/archive","patch","DEPENDENTS_DEPENDENT_ARCHIVE"),
                List.of("/admin/tools/{id}/archive","patch","TOOLS_TOOL_ARCHIVE"),
                List.of("/vehicles/archive/admin/{vehicleId}","patch","VEHICLES_VEHICLE_ARCHIVE"),
                List.of("/billing/charge-definitions/{id}","delete","BILLING_CHARGE_DEFINITION_DELETE"))) {
            assertThat(document.path("paths").path(contract.get(0)).path(contract.get(1)).path("x-required-permissions").get(0).asText())
                    .isEqualTo(contract.get(2));
        }
        for(var contract:List.of(List.of("/identity/admin/users/{id}/deletion-history","UserDeletionHistorySnapshot"),
                List.of("/authorization/roles/{id}/deletion-history","RoleDeletionHistorySnapshot"),
                List.of("/billing/charge-definitions/{id}/deletion-history","ChargeDefinitionDeletionHistorySnapshot"))) {
            assertThat(document.path("paths").path(contract.get(0)).path("get").path("responses").path("200")
                    .path("content").path("application/json").path("schema").path("$ref").asText())
                    .isEqualTo("#/components/schemas/"+contract.get(1));
        }
    }

    @Test void unusedDefinitionDeletionStoresFullConfiguration() {
        Long id=definition(); definitions.deactivate(id); definitions.archive(id); definitions.delete(id,1L);
        var snapshot=definitionHistory.find(id,1L);
        assertThat(snapshot.defaultAmount()).isEqualByComparingTo("125.50");
        assertThat(snapshot.previousStatus()).isEqualTo("ARCHIVED");
        assertThat(snapshot.description()).isEqualTo("retained config");
        assertThatThrownBy(() -> definitions.findById(id)).isInstanceOf(RuntimeException.class);
    }

    @Test void duplicateConcurrentUserArchivalProducesOneTransitionAndStableTimestamp() throws Exception {
        Long id=user(false); var executor=Executors.newFixedThreadPool(2); var start=new CountDownLatch(1);
        try {
            Callable<UserDetails> task=() -> { assertThat(start.await(10,TimeUnit.SECONDS)).isTrue(); return identity.archive(id,id,CREATED.plusSeconds(60)); };
            var first=executor.submit(task); var second=executor.submit(task); start.countDown();
            assertThat(first.get(20,TimeUnit.SECONDS).status()).isEqualTo(UserStatus.ARCHIVED);
            assertThat(second.get(20,TimeUnit.SECONDS).archivedAt()).isEqualTo(CREATED.plusSeconds(60));
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_logs WHERE actor_id=? AND action LIKE 'IDENTITY_USER_ARCHIVE:%'",Long.class,id)).isEqualTo(1);
        } finally { start.countDown(); executor.shutdownNow(); }
    }

    @Test void archivedRoleStopsGrantingPermissionToAnExistingSessionUntilExplicitActivation() throws Exception {
        Long actor=user(false), target=user(false); String token=access(actor);
        Long role=grant(actor,com.jeepclub.backend.shared.authorization.PermissionCode.IDENTITY_USER_ARCHIVE);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/identity/admin/users/"+target+"/archive")
                .header("Authorization","Bearer "+token)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        roles.archiveRole(role,actor);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/identity/admin/users/"+target+"/archive")
                .header("Authorization","Bearer "+token)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
        roles.unarchiveRole(role,actor);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/identity/admin/users/"+target+"/archive")
                .header("Authorization","Bearer "+token)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
        roles.activateRole(role,actor);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/identity/admin/users/"+target+"/archive")
                .header("Authorization","Bearer "+token)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }

    @Test void archivingIdentityImmediatelyInvalidatesItsExistingAccessSession() throws Exception {
        Long actor=user(false), target=user(false); String token=access(actor);
        grant(actor,com.jeepclub.backend.shared.authorization.PermissionCode.IDENTITY_USER_ARCHIVE);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/identity/admin/users/"+target+"/archive")
                .header("Authorization","Bearer "+token)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        identity.archive(actor,target,CREATED.plusSeconds(60));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/identity/admin/users/"+target+"/archive")
                .header("Authorization","Bearer "+token)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test void historyRequiresItsOwnPermissionAndUsesNoStore() throws Exception {
        Long pending=user(true), reader=user(false); String token=access(reader);
        identity.delete(pending,reader,CREATED.plusSeconds(60));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/identity/admin/users/"+pending+"/deletion-history")
                .header("Authorization","Bearer "+token)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
        grant(reader,com.jeepclub.backend.shared.authorization.PermissionCode.IDENTITY_USER_HISTORY_READ);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/identity/admin/users/"+pending+"/deletion-history")
                .header("Authorization","Bearer "+token)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Cache-Control","no-store"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.originalId").value(pending))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.cpf").doesNotExist())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.passwordHash").doesNotExist());
    }

    @Test void dependentArchiveEnforcesOwnershipAndRetainsArchivedReadingForOwner() throws Exception {
        Long owner=user(false), stranger=user(false);
        var dependent=dependents.create("Synthetic dependent",String.valueOf(IDS.incrementAndGet()),LocalDate.of(2010,1,1),RelationshipType.CHILD,null,owner);
        String strangerToken=access(stranger), ownerToken=access(owner);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/dependents/"+dependent.id()+"/archive")
                .header("Authorization","Bearer "+strangerToken)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
        assertThat(dependents.findById(dependent.id(),owner).status()).isEqualTo(DependentStatus.ACTIVE);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/dependents/"+dependent.id()+"/archive")
                .header("Authorization","Bearer "+ownerToken)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        assertThat(dependents.findById(dependent.id(),owner).status()).isEqualTo(DependentStatus.ARCHIVED);
    }

    @Test void rootUserAndRoleAreProtectedFromArchivalAndPhysicalDeletion() {
        Long user=user(true), root=rootRoles.ensureRootRole();
        bootstrapRoles.assignRoleToUserIfMissing(user,root);
        assertThatThrownBy(() -> identity.archive(user,user,CREATED.plusSeconds(60))).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> identity.delete(user,user,CREATED.plusSeconds(60))).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> roles.archiveRole(root,user)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> roles.deleteRole(root,user)).isInstanceOf(RuntimeException.class);
        assertThat(users.findById(user).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(roleRepository.findById(root).orElseThrow().getStatus()).isEqualTo(RoleStatus.ACTIVE);
    }

    @Test void liveEventReferencesPreventVehicleAndDependentDeletion() {
        Long owner=user(false); long unique=IDS.incrementAndGet();
        var vehicle=vehicles.createForOwner("Retained",null,"EVT"+String.format("%04d",unique%10000),String.valueOf(unique),"Jeep","Test",2020,2020,"Blue",5,FuelType.DIESEL,2d,false,owner);
        var dependent=dependents.create("Retained dependent",String.valueOf(IDS.incrementAndGet()),LocalDate.of(2010,1,1),RelationshipType.CHILD,null,owner);
        var event=tx(() -> {
            var e=new com.jeepclub.backend.publications.infra.persistence.entity.EventEntity();
            e.setAuthorUserId(owner); e.setTitle("Synthetic retained Event"); e.setContent("Synthetic test only");
            e.setStatus(com.jeepclub.backend.publications.core.domain.enums.PublicationStatus.PUBLISHED);
            e.setCreatedAt(CREATED); e.setUpdatedAt(CREATED); e.setPublishedAt(CREATED); e.setStartsAt(Instant.now().plusSeconds(86400));
            entityManager.persist(e); entityManager.flush();
            eventReferences.save(new com.jeepclub.backend.publications.core.domain.model.EventRegistration(null,e.getId(),owner,
                    com.jeepclub.backend.publications.core.domain.model.EventRegistration.Status.CONFIRMED,
                    List.of(new com.jeepclub.backend.publications.core.domain.model.EventRegistration.Allocation(vehicle.getId(),true,List.of(dependent.id()))),CREATED,CREATED,null));
            return e.getId();
        });
        assertThatThrownBy(() -> vehicles.delete(vehicle.getId(),owner)).isInstanceOf(com.jeepclub.backend.vehicles.core.domain.exception.VehicleLifecycleConflictException.class);
        assertThatThrownBy(() -> dependents.delete(dependent.id(),owner)).isInstanceOf(com.jeepclub.backend.dependents.core.domain.exception.DependentLifecycleConflictException.class);
        assertThat(vehicles.findById(vehicle.getId()).getId()).isEqualTo(vehicle.getId());
        assertThat(dependents.findById(dependent.id(),owner).id()).isEqualTo(dependent.id());
        assertThat(tx(() -> eventReferences.registrations(event))).hasSize(1);
    }

    @Autowired jakarta.persistence.EntityManager entityManager;
    @Autowired com.jeepclub.backend.publications.core.repository.EventOperationsRepository eventReferences;
}
