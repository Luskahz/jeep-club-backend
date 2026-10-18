package com.jeepclub.backend.memberships.registry;

import com.jeepclub.backend.iam.identity.api.module.*;
import com.jeepclub.backend.iam.authorization.core.domain.model.*;
import com.jeepclub.backend.iam.authorization.core.repository.*;
import com.jeepclub.backend.memberships.core.application.service.member.AdminMemberService;
import com.jeepclub.backend.memberships.api.module.MemberRegistrationQuery;
import com.jeepclub.backend.memberships.infra.persistence.jpa.MemberRegistrySequenceJpaRepository;
import com.jeepclub.backend.shared.authorization.PermissionCode;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class MemberRegistryApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRegistration registration;
    @Autowired UserQuery users;
    @Autowired RoleRepository roles;
    @Autowired PermissionRepository permissions;
    @Autowired UserRoleRepository userRoles;
    @Autowired RolePermissionRepository rolePermissions;
    @Autowired MemberRegistrySequenceJpaRepository sequence;
    @Autowired AdminMemberService registry;
    @Autowired MemberRegistrationQuery query;
    private static final AtomicInteger SEQUENCE=new AtomicInteger(920000000);
    private record Actor(Long id,String token) { }
    @BeforeEach void coordinationFixture() {
        if(!sequence.existsById(1L)) { var row=new com.jeepclub.backend.memberships.infra.persistence.entity.MemberRegistrySequenceEntity();row.setId(1L);sequence.saveAndFlush(row); }
    }
    private Actor actor(PermissionCode... codes) {
        int seed=SEQUENCE.incrementAndGet();String cpf=MemberRegistryScenarios.cpf(seed);
        var tokens=registration.registerAndAuthenticate(new UserRegistrationData("Synthetic API registry",null,null,cpf,null,null,null,Instant.now()),"test-password-123");
        Long id=users.findByCpf(cpf).orElseThrow().id();
        if(codes.length>0) {
            var role=roles.save(Role.create("registry-"+seed,"Synthetic test",Instant.now()));userRoles.save(UserRole.create(id,role.getId(),Instant.now()));
            for(var code:codes)rolePermissions.save(RolePermission.create(role.getId(),permissions.findByCode(code).orElseThrow().getId(),Instant.now()));
        }
        return new Actor(id,"Bearer "+tokens.accessToken());
    }
    private Actor admin() { return actor(PermissionCode.MEMBERSHIP_MEMBER_REGISTRATION_MANAGE,PermissionCode.IDENTITY_USER_READ,PermissionCode.IDENTITY_USER_EXPORT); }
    private String assignment(String number) { return "{\"membershipNumber\":\""+number+"\",\"reason\":\"Synthetic verified source\"}"; }
    @Test void authenticationAndPermissionAreEnforcedByRealSecurity() throws Exception {
        mvc.perform(get("/admin/members")).andExpect(status().isUnauthorized());
        var ordinary=actor();mvc.perform(get("/admin/members").header("Authorization",ordinary.token())).andExpect(status().isForbidden());
        var a=admin();mvc.perform(get("/admin/members").header("Authorization",a.token())).andExpect(status().isOk());
        mvc.perform(get("/admin/members/by-identity/{id}/history",ordinary.id()).header("Authorization",ordinary.token())).andExpect(status().isForbidden());
    }
    @Test void assignmentCorrectionAndAllIdentityReadsResolveCurrentNumber() throws Exception {
        var a=admin();var member=actor();String old="API-"+member.id()+"-027",updated="API-"+member.id()+"-037";
        var before=users.findById(member.id()).orElseThrow();
        String response=mvc.perform(put("/admin/members/by-identity/{id}",member.id()).header("Authorization",a.token()).contentType(MediaType.APPLICATION_JSON).content(assignment(old)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.identityUserId").value(member.id())).andReturn().getResponse().getContentAsString();
        Number version=JsonPath.read(response,"$.version");
        mvc.perform(patch("/admin/members/by-identity/{id}/membership-number",member.id()).header("Authorization",a.token()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"membershipNumber\":\""+updated+"\",\"expectedVersion\":"+version+",\"reason\":\"Synthetic correction\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value((Number)JsonPath.read(response,"$.id"))).andExpect(jsonPath("$.membershipNumber").value(updated));
        mvc.perform(get("/identity/me").header("Authorization",member.token())).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(member.id())).andExpect(jsonPath("$.membershipNumber").value(updated));
        mvc.perform(get("/identity/me/profile").header("Authorization",member.token())).andExpect(status().isOk()).andExpect(jsonPath("$.membershipNumber").value(updated));
        mvc.perform(get("/identity/admin/users/{id}",member.id()).header("Authorization",a.token())).andExpect(status().isOk()).andExpect(jsonPath("$.membershipNumber").value(updated));
        mvc.perform(get("/identity/admin/users").param("id",member.id().toString()).header("Authorization",a.token())).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].membershipNumber").value(updated));
        mvc.perform(get("/identity/admin/users/export").param("id",member.id().toString()).header("Authorization",a.token())).andExpect(status().isOk()).andExpect(content().string(containsString(updated)));
        mvc.perform(get("/admin/members/by-identity/{id}/history",member.id()).header("Authorization",a.token())).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2)).andExpect(jsonPath("$.content[0].previousNumber").value(old));
        assertThat(users.findById(member.id())).contains(before);
    }
    @Test void missingMembershipNeverBlocksLoginOrReturnsTechnicalIdAsNumber() throws Exception {
        var a=actor();mvc.perform(get("/identity/me").header("Authorization",a.token())).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(a.id())).andExpect(jsonPath("$.membershipNumber").isEmpty());
        assertThat(query.byIdentityUserId(a.id())).isEmpty();
    }
    @Test void invalidNotFoundAndUniquenessErrorsAreSanitized() throws Exception {
        var a=admin();var target=actor();
        mvc.perform(put("/admin/members/by-identity/{id}",target.id()).header("Authorization",a.token()).contentType(MediaType.APPLICATION_JSON).content(assignment(" "))).andExpect(status().isBadRequest());
        mvc.perform(put("/admin/members/by-identity/{id}",Long.MAX_VALUE).header("Authorization",a.token()).contentType(MediaType.APPLICATION_JSON).content(assignment("UNKNOWN"))).andExpect(status().isNotFound());
        String number="CONF-"+target.id();registry.assign(target.id(),number,a.id(),"Synthetic source");
        mvc.perform(put("/admin/members/by-identity/{id}",a.id()).header("Authorization",a.token()).contentType(MediaType.APPLICATION_JSON).content(assignment(number)))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEMBER_REGISTRY_CONFLICT"))
            .andExpect(content().string(org.hamcrest.Matchers.not(containsString("insert into"))));
        mvc.perform(patch("/admin/members/by-identity/{id}/membership-number",target.id()).header("Authorization",a.token()).contentType(MediaType.APPLICATION_JSON).content("{\"membershipNumber\":\"NEW\",\"expectedVersion\":-1,\"reason\":\"Test\"}"))
            .andExpect(status().isBadRequest());
    }
    @Test void reservedNumberCannotBeAssignedAndGenerationIsDisabledByDefault() throws Exception {
        var a=admin();String number="RES-"+a.id();
        mvc.perform(post("/admin/members/reservations").header("Authorization",a.token()).contentType(MediaType.APPLICATION_JSON).content(assignment(number))).andExpect(status().isCreated());
        mvc.perform(put("/admin/members/by-identity/{id}",a.id()).header("Authorization",a.token()).contentType(MediaType.APPLICATION_JSON).content(assignment(number))).andExpect(status().isConflict());
        mvc.perform(post("/admin/members/by-identity/{id}/generate",a.id()).header("Authorization",a.token()).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Test\"}")).andExpect(status().isConflict());
        assertThat(query.byIdentityUserId(a.id())).isEmpty();
    }
    @Test void importRequiresPreviewAndReusesRealAdministrativeTransactions() throws Exception {
        var a=admin();var target=actor();String rows="\"rows\":[{\"identityUserId\":"+target.id()+",\"membershipNumber\":\"IMP-"+target.id()+"\",\"verified\":true,\"verificationReference\":\"synthetic-verified-reference\"}],\"reason\":\"Synthetic source\"";
        mvc.perform(post("/admin/members/imports/apply").header("Authorization",a.token()).contentType(MediaType.APPLICATION_JSON).content("{"+rows+"}" )).andExpect(status().isBadRequest());
        var result=mvc.perform(post("/admin/members/imports/dry-run").header("Authorization",a.token()).contentType(MediaType.APPLICATION_JSON).content("{"+rows+"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.rows[0].outcome").value("READY")).andReturn().getResponse().getContentAsString();
        assertThat(query.byIdentityUserId(target.id())).isEmpty();String proof=JsonPath.read(result,"$.dryRunToken");
        mvc.perform(post("/admin/members/imports/apply").header("Authorization",a.token()).contentType(MediaType.APPLICATION_JSON).content("{"+rows+",\"dryRunToken\":\""+proof+"\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.rows[0].outcome").value("APPLIED"));
    }
    @Test void badPathSortAndEmptyImportAreRejected() throws Exception {
        var a=admin();
        mvc.perform(get("/admin/members/by-identity/0").header("Authorization",a.token())).andExpect(status().isBadRequest());
        mvc.perform(get("/admin/members").param("sort","secret,asc").header("Authorization",a.token())).andExpect(status().isBadRequest());
        mvc.perform(post("/admin/members/imports/dry-run").header("Authorization",a.token()).contentType(MediaType.APPLICATION_JSON).content("{\"rows\":[],\"reason\":\"Synthetic\"}")).andExpect(status().isBadRequest());
    }
    @Test void openApiPublishesMatchingPermissionAndNewDtoProperty() throws Exception {
        var response=mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(response).contains("MEMBERSHIP_MEMBER_REGISTRATION_MANAGE","/admin/members/by-identity/{userId}","membershipNumber","expectedVersion");
        var extension=JsonPath.read(response,"$.paths['/admin/members'].get['x-required-permissions']");
        assertThat(extension.toString()).contains("MEMBERSHIP_MEMBER_REGISTRATION_MANAGE");
    }
}
