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

@SpringBootTest(properties="memberships.registry.imports.apply-enabled=true") @AutoConfigureMockMvc @ActiveProfiles("test")
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
    private Actor admin() { return actor(PermissionCode.MEMBERSHIP_MEMBER_REGISTRATION_MANAGE,PermissionCode.MEMBERSHIP_MEMBER_READ,PermissionCode.MEMBERSHIP_MEMBER_AUDIT_READ,PermissionCode.MEMBERSHIP_MEMBER_NUMBER_RESERVE,PermissionCode.MEMBERSHIP_MEMBER_NUMBER_ISSUE,PermissionCode.MEMBERSHIP_MEMBER_IMPORT,PermissionCode.IDENTITY_USER_READ,PermissionCode.IDENTITY_USER_EXPORT); }
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
        var a=admin();var target=actor();String rows="\"rows\":[{\"identityUserId\":"+target.id()+",\"membershipNumber\":\"IMP-"+target.id()+"\",\"administrativelyConfirmed\":true,\"sourceReference\":\"synthetic-verified-reference\"}],\"reason\":\"Synthetic source\"";
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
        assertThat(extension.toString()).contains("MEMBERSHIP_MEMBER_READ");
    }

    @Test void fieldsSelectMembershipWithoutExposingAuxiliaryId() throws Exception {
        var a=admin(); var target=actor(); String number="FIELDS-"+target.id()+"-027";
        var member=registry.assign(target.id(),number,a.id(),"Synthetic source");
        for(String fields:java.util.List.of("MEMBERSHIP_NUMBER","NAME,MEMBERSHIP_NUMBER")) {
            var response=mvc.perform(get("/identity/admin/users").param("id",target.id().toString()).param("fields",fields)
                    .header("Authorization",a.token())).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].membershipNumber").value(number))
                    .andExpect(jsonPath("$.content[0].id").doesNotExist()).andReturn().getResponse().getContentAsString();
            java.util.Map<String,Object> item=JsonPath.read(response,"$.content[0]");
            assertThat(item).hasSize(fields.contains("NAME")?2:1);
        }
        mvc.perform(get("/identity/admin/users").param("id",target.id().toString()).param("fields","ID,NAME")
                .header("Authorization",a.token())).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(target.id())).andExpect(jsonPath("$.content[0].membershipNumber").doesNotExist());
        mvc.perform(get("/identity/admin/users").param("id",a.id().toString()).param("fields","MEMBERSHIP_NUMBER")
                .header("Authorization",a.token())).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").doesNotExist())
                .andExpect(jsonPath("$.content[0].membershipNumber").doesNotExist());
        registry.correct(target.id(),number+"X",member.version(),a.id(),"Synthetic correction");
        mvc.perform(get("/identity/admin/users").param("id",target.id().toString()).param("fields","MEMBERSHIP_NUMBER")
                .header("Authorization",a.token())).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].membershipNumber").value(number+"X"));
    }
    @Test void membershipSearchComposesFiltersAndUsesOnlyCurrentNumber() throws Exception {
        var a=admin();var target=actor();String number="LOOKUP-"+target.id()+"-027";
        var member=registry.assign(target.id(),number,a.id(),"Synthetic source");
        for(String key:java.util.List.of("membershipNumber","q")) {
            mvc.perform(get("/identity/admin/users").param(key,number).param("name","Synthetic").param("status","ACTIVE")
                    .param("fields","NAME,MEMBERSHIP_NUMBER").param("size","1").header("Authorization",a.token()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].membershipNumber").value(number)).andExpect(jsonPath("$.content[0].id").doesNotExist());
            mvc.perform(get("/identity/admin/users").param(key,number).param("name","no-match-synthetic")
                    .header("Authorization",a.token())).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
            mvc.perform(get("/identity/admin/users").param(key,number).param("status","INACTIVE")
                    .header("Authorization",a.token())).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        }
        mvc.perform(get("/identity/admin/users").param("membershipNumber",number).param("page","1").param("size","1")
                .header("Authorization",a.token())).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.empty").value(true));
        mvc.perform(get("/identity/admin/users").param("membershipNumber",number).param("id",a.id().toString())
                .header("Authorization",a.token())).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        registry.correct(target.id(),number+"X",member.version(),a.id(),"Synthetic correction");
        for(String key:java.util.List.of("membershipNumber","q")) {
            mvc.perform(get("/identity/admin/users").param(key,number).header("Authorization",a.token()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
            mvc.perform(get("/identity/admin/users").param(key,number+"X").header("Authorization",a.token()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].membershipNumber").value(number+"X"));
        }
        mvc.perform(get("/identity/admin/users").param("membershipNumber","UNKNOWN-SYNTHETIC")
                .header("Authorization",a.token())).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/identity/admin/users").param("sort","membershipNumber,asc").header("Authorization",a.token())).andExpect(status().isBadRequest());
        mvc.perform(get("/identity/admin/users").param("membershipNumber",number+"X").header("Authorization",actor().token())).andExpect(status().isForbidden());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value=PermissionCode.class,names={"MEMBERSHIP_MEMBER_READ","MEMBERSHIP_MEMBER_REGISTRATION_MANAGE","MEMBERSHIP_MEMBER_AUDIT_READ","MEMBERSHIP_MEMBER_NUMBER_RESERVE","MEMBERSHIP_MEMBER_NUMBER_ISSUE","MEMBERSHIP_MEMBER_IMPORT"})
    void isolatedRegistryPermissionsDoNotGrantOtherOperations(PermissionCode code) throws Exception {
        var a=actor(code);String number="RBAC-"+a.id();registry.assign(a.id(),number,a.id(),"Synthetic source");
        mvc.perform(get("/admin/members").header("Authorization",a.token())).andExpect(status().is(code==PermissionCode.MEMBERSHIP_MEMBER_READ?200:403));
        mvc.perform(put("/admin/members/by-identity/{id}",a.id()).header("Authorization",a.token()).contentType(MediaType.APPLICATION_JSON).content(assignment(number)))
                .andExpect(status().is(code==PermissionCode.MEMBERSHIP_MEMBER_REGISTRATION_MANAGE?200:403));
        mvc.perform(patch("/admin/members/by-identity/{id}/membership-number",a.id()).header("Authorization",a.token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"membershipNumber\":\""+number+"\",\"expectedVersion\":0,\"reason\":\"Synthetic\"}"))
                .andExpect(status().is(code==PermissionCode.MEMBERSHIP_MEMBER_REGISTRATION_MANAGE?200:403));
        mvc.perform(get("/admin/members/by-identity/{id}/history",a.id()).header("Authorization",a.token()))
                .andExpect(status().is(code==PermissionCode.MEMBERSHIP_MEMBER_AUDIT_READ?200:403));
        mvc.perform(post("/admin/members/reservations").header("Authorization",a.token()).contentType(MediaType.APPLICATION_JSON).content(assignment(number+"R")))
                .andExpect(status().is(code==PermissionCode.MEMBERSHIP_MEMBER_NUMBER_RESERVE?201:403));
        mvc.perform(post("/admin/members/by-identity/{id}/generate",a.id()).header("Authorization",a.token()).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Synthetic\"}"))
                .andExpect(status().is(code==PermissionCode.MEMBERSHIP_MEMBER_NUMBER_ISSUE?409:403));
        String rows="{\"rows\":[{\"identityUserId\":"+a.id()+",\"membershipNumber\":\""+number+"\",\"administrativelyConfirmed\":true,\"sourceReference\":\"synthetic-source\"}],\"reason\":\"Synthetic\"}";
        mvc.perform(post("/admin/members/imports/dry-run").header("Authorization",a.token()).contentType(MediaType.APPLICATION_JSON).content(rows))
                .andExpect(status().is(code==PermissionCode.MEMBERSHIP_MEMBER_IMPORT?200:403));
        mvc.perform(post("/admin/members/imports/apply").header("Authorization",a.token()).contentType(MediaType.APPLICATION_JSON).content(rows))
                .andExpect(status().is(code==PermissionCode.MEMBERSHIP_MEMBER_IMPORT?400:403));
    }
    @Test void exportPrioritizesCurrentBusinessNumberAndOffersTechnicalCompatibility() throws Exception {
        var a=admin();var target=actor();String number="EXPORT-"+target.id()+"-027";
        var member=registry.assign(target.id(),number,a.id(),"Synthetic source");
        registry.correct(target.id(),number+"X",member.version(),a.id(),"Synthetic correction");
        for(String layout:java.util.List.of("MEMBER","TECHNICAL")) {
            var csv=mvc.perform(get("/identity/admin/users/export").param("id",target.id().toString()).param("layout",layout)
                    .header("Authorization",a.token())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            assertThat(csv).contains(number+"X");
            String header=csv.lines().findFirst().orElseThrow().replace("\uFEFF","").replace("\"","");
            if(layout.equals("MEMBER"))assertThat(header).startsWith("Matrícula;").endsWith("ID técnico");
            else assertThat(header).startsWith("ID;Matrícula;Nome;");
            var bytes=mvc.perform(get("/identity/admin/users/export").param("id",target.id().toString()).param("format","PDF").param("layout",layout)
                    .header("Authorization",a.token())).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
            try(var pdf=org.apache.pdfbox.Loader.loadPDF(bytes)) {
                String text=new org.apache.pdfbox.text.PDFTextStripper().getText(pdf);
                assertThat(text).contains(number+"X","Matrícula");
                if(layout.equals("MEMBER"))assertThat(text.indexOf("Matrícula")).isLessThan(text.lastIndexOf("ID técnico"));
            }
        }
        var csv=mvc.perform(get("/identity/admin/users/export").param("id",a.id().toString()).header("Authorization",a.token()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv.lines().skip(1).findFirst().orElseThrow()).startsWith("\"\";");
    }
    @Test void lifecycleAndRegistrationChangesPreserveRegistryAndClaims() throws Exception {
        var a=actor(PermissionCode.IDENTITY_USER_UPDATE,PermissionCode.IDENTITY_USER_DISABLE,PermissionCode.IDENTITY_USER_ENABLE);
        var target=actor();String number="LIFE-"+target.id();registry.assign(target.id(),number,a.id(),"Synthetic source");var before=registry.byIdentity(target.id());
        mvc.perform(patch("/identity/admin/users/{id}/disable",target.id()).header("Authorization",a.token())).andExpect(status().isOk());
        assertThat(registry.byIdentity(target.id())).isEqualTo(before);
        mvc.perform(patch("/identity/admin/users/{id}/enable",target.id()).header("Authorization",a.token())).andExpect(status().isOk());
        mvc.perform(patch("/identity/admin/users/{id}",target.id()).header("Authorization",a.token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Synthetic corrected name\",\"email\":\"synthetic-"+target.id()+"@example.test\",\"cpf\":\""+MemberRegistryScenarios.cpf(SEQUENCE.incrementAndGet())+"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.membershipNumber").value(number));
        assertThat(registry.byIdentity(target.id())).isEqualTo(before);
        assertThat(registry.history(target.id(),org.springframework.data.domain.PageRequest.of(0,20)).getTotalElements()).isEqualTo(1);
        assertThatThrownBy(() -> registry.assign(a.id(),number,a.id(),"Synthetic attempted reuse")).isInstanceOf(com.jeepclub.backend.memberships.core.domain.exception.MemberRegistryException.class);
    }
    @Autowired com.jeepclub.backend.iam.authorization.core.application.service.bootstrap.RootRoleBootstrapService rootBootstrap;
    @Test void rootReceivesNewPermissionsStructurallyAndCustomDoesNot() throws Exception {
        var a=actor();Long root=rootBootstrap.ensureRootRole();userRoles.save(UserRole.create(a.id(),root,Instant.now()));
        mvc.perform(get("/admin/members").header("Authorization",a.token())).andExpect(status().isOk());
        for(var code:PermissionCode.values()) if(code.name().startsWith("MEMBERSHIP_MEMBER_"))
            assertThat(rolePermissions.existsByRoleIdAndPermissionId(root,permissions.findByCode(code).orElseThrow().getId())).isTrue();
        var custom=actor(PermissionCode.MEMBERSHIP_MEMBER_REGISTRATION_MANAGE);
        rootBootstrap.ensureRootRole();
        mvc.perform(post("/admin/members/reservations").header("Authorization",custom.token()).contentType(MediaType.APPLICATION_JSON).content(assignment("ROOT-CHECK-"+custom.id())))
                .andExpect(status().isForbidden());
    }
    @Test void openApiDocumentsEveryOperationPermissionAndFieldSelection() throws Exception {
        String json=mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String[][] operations={{"/admin/members","get","MEMBERSHIP_MEMBER_READ"},
            {"/admin/members/by-number","get","MEMBERSHIP_MEMBER_READ"},
            {"/admin/members/by-identity/{userId}","get","MEMBERSHIP_MEMBER_READ"},
            {"/admin/members/by-identity/{userId}","put","MEMBERSHIP_MEMBER_REGISTRATION_MANAGE"},
            {"/admin/members/by-identity/{userId}/membership-number","patch","MEMBERSHIP_MEMBER_REGISTRATION_MANAGE"},
            {"/admin/members/by-identity/{userId}/history","get","MEMBERSHIP_MEMBER_AUDIT_READ"},
            {"/admin/members/by-identity/{userId}/generate","post","MEMBERSHIP_MEMBER_NUMBER_ISSUE"},
            {"/admin/members/reservations","post","MEMBERSHIP_MEMBER_NUMBER_RESERVE"},
            {"/admin/members/imports/dry-run","post","MEMBERSHIP_MEMBER_IMPORT"},
            {"/admin/members/imports/apply","post","MEMBERSHIP_MEMBER_IMPORT"}};
        for(var op:operations) {
            java.util.List<String> actual=JsonPath.read(json,"$.paths['"+op[0]+"']."+op[1]+"['x-required-permissions']");
            assertThat(actual).containsExactly(op[2]);
        }
        assertThat(json).contains("MEMBERSHIP_NUMBER","membershipNumber","administrativelyConfirmed","sourceReference","TECHNICAL");
    }
    @Test void importOnlyPermissionAppliesConfirmedRowsButNotMissingDeclaration() throws Exception {
        var a=actor(PermissionCode.MEMBERSHIP_MEMBER_IMPORT);var target=actor();
        String row="{\"identityUserId\":"+target.id()+",\"membershipNumber\":\"IMPORT-ONLY-"+target.id()+"\",\"sourceReference\":\"synthetic/v1/line-1\"}";
        String missing="{\"rows\":["+row+"],\"reason\":\"Synthetic source\"}";
        mvc.perform(post("/admin/members/imports/dry-run").header("Authorization",a.token()).contentType(MediaType.APPLICATION_JSON).content(missing))
                .andExpect(status().isBadRequest());
        assertThat(query.byIdentityUserId(target.id())).isEmpty();
        String unconfirmed=missing.replace("\"sourceReference\"","\"administrativelyConfirmed\":false,\"sourceReference\"");
        String preview=mvc.perform(post("/admin/members/imports/dry-run").header("Authorization",a.token()).contentType(MediaType.APPLICATION_JSON).content(unconfirmed))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rows[0].outcome").value("UNVERIFIED")).andReturn().getResponse().getContentAsString();
        String proof=JsonPath.read(preview,"$.dryRunToken");
        mvc.perform(post("/admin/members/imports/apply").header("Authorization",a.token()).contentType(MediaType.APPLICATION_JSON)
                .content(unconfirmed.substring(0,unconfirmed.length()-1)+",\"dryRunToken\":\""+proof+"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rows[0].outcome").value("UNVERIFIED"));
        assertThat(query.byIdentityUserId(target.id())).isEmpty();
        String confirmed=unconfirmed.replace("\"administrativelyConfirmed\":false","\"administrativelyConfirmed\":true");
        preview=mvc.perform(post("/admin/members/imports/dry-run").header("Authorization",a.token()).contentType(MediaType.APPLICATION_JSON).content(confirmed))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rows[0].outcome").value("READY")).andReturn().getResponse().getContentAsString();
        proof=JsonPath.read(preview,"$.dryRunToken");
        mvc.perform(post("/admin/members/imports/apply").header("Authorization",a.token()).contentType(MediaType.APPLICATION_JSON)
                .content(confirmed.substring(0,confirmed.length()-1)+",\"dryRunToken\":\""+proof+"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rows[0].outcome").value("APPLIED"));
    }
}
