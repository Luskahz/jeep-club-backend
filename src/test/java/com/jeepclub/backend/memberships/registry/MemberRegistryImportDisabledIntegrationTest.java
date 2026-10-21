package com.jeepclub.backend.memberships.registry;

import com.jeepclub.backend.iam.identity.api.module.*;
import com.jeepclub.backend.iam.authorization.core.domain.model.*;
import com.jeepclub.backend.iam.authorization.core.repository.*;
import com.jeepclub.backend.shared.authorization.PermissionCode;
import com.jeepclub.backend.memberships.infra.config.MemberImportProperties;
import com.jeepclub.backend.memberships.infra.config.MemberIssuanceProperties;
import com.jeepclub.backend.memberships.infra.persistence.jpa.MemberRegistrySequenceJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class MemberRegistryImportDisabledIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRegistration registration;
    @Autowired UserQuery users;
    @Autowired RoleRepository roles;
    @Autowired PermissionRepository permissions;
    @Autowired UserRoleRepository userRoles;
    @Autowired RolePermissionRepository rolePermissions;
    @Autowired JdbcTemplate jdbc;
    @Autowired MemberImportProperties imports;
    @Autowired MemberIssuanceProperties issuance;
    @Test void defaultStartupAllowsDryRunButRefusesApplyEvenWithImportPermissionWithoutWrites() throws Exception {
        assertThat(imports.applyEnabled()).isFalse();assertThat(issuance.enabled()).isFalse();
        String cpf=MemberRegistryScenarios.cpf(930000001);
        var tokens=registration.registerAndAuthenticate(new UserRegistrationData("Synthetic disabled import",null,null,cpf,null,null,null,Instant.now()),"synthetic-password");
        Long id=users.findByCpf(cpf).orElseThrow().id();
        var role=roles.save(Role.create("synthetic-import-only","Synthetic",Instant.now()));
        userRoles.save(UserRole.create(id,role.getId(),Instant.now()));
        rolePermissions.save(RolePermission.create(role.getId(),permissions.findByCode(PermissionCode.MEMBERSHIP_MEMBER_IMPORT).orElseThrow().getId(),Instant.now()));
        String body="{\"rows\":[{\"identityUserId\":"+id+",\"membershipNumber\":\"027\",\"administrativelyConfirmed\":true,\"sourceReference\":\"synthetic/v1/line-1\"}],\"reason\":\"Synthetic source\"}";
        long members=jdbc.queryForObject("select count(*) from membership_members",Long.class),audits=jdbc.queryForObject("select count(*) from membership_member_audits",Long.class),claims=jdbc.queryForObject("select count(*) from membership_number_claims",Long.class);
        String response=mvc.perform(post("/admin/members/imports/dry-run").header("Authorization","Bearer "+tokens.accessToken()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rows[0].outcome").value("READY")).andReturn().getResponse().getContentAsString();
        String proof=com.jayway.jsonpath.JsonPath.read(response,"$.dryRunToken");
        mvc.perform(post("/admin/members/imports/apply").header("Authorization","Bearer "+tokens.accessToken()).contentType(MediaType.APPLICATION_JSON)
                .content(body.substring(0,body.length()-1)+",\"dryRunToken\":\""+proof+"\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEMBER_REGISTRY_IMPORT_DISABLED"));
        assertThat(jdbc.queryForObject("select count(*) from membership_members",Long.class)).isEqualTo(members);
        assertThat(jdbc.queryForObject("select count(*) from membership_member_audits",Long.class)).isEqualTo(audits);
        assertThat(jdbc.queryForObject("select count(*) from membership_number_claims",Long.class)).isEqualTo(claims);
    }
}
