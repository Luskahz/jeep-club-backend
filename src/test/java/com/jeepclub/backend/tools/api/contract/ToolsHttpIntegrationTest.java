package com.jeepclub.backend.tools.api.contract;

import com.jeepclub.backend.iam.authorization.core.domain.model.Role;
import com.jeepclub.backend.iam.authorization.core.domain.model.RolePermission;
import com.jeepclub.backend.iam.authorization.core.domain.model.UserRole;
import com.jeepclub.backend.iam.authorization.core.repository.PermissionRepository;
import com.jeepclub.backend.iam.authorization.core.repository.RolePermissionRepository;
import com.jeepclub.backend.iam.authorization.core.repository.RoleRepository;
import com.jeepclub.backend.iam.authorization.core.repository.UserRoleRepository;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import com.jeepclub.backend.iam.identity.api.module.UserRegistration;
import com.jeepclub.backend.iam.identity.api.module.UserRegistrationData;
import com.jeepclub.backend.shared.authorization.PermissionCode;
import com.jeepclub.backend.tools.infra.persistence.jpa.ToolHistoryJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ToolsHttpIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired Clock clock;
    @Autowired UserRegistration registration;
    @Autowired UserQuery users;
    @Autowired RoleRepository roles;
    @Autowired UserRoleRepository userRoles;
    @Autowired PermissionRepository permissions;
    @Autowired RolePermissionRepository rolePermissions;
    @Autowired ToolHistoryJpaRepository history;

    @Test
    @Transactional
    void memberCannotReadOrMutateAnotherMembersToolAndDeleteArchivesSnapshot() throws Exception {
        String owner = token("Tools Owner", "14739528673");
        String other = token("Tools Other", "92583617419");

        mvc.perform(get("/tools")).andExpect(status().isUnauthorized());

        mvc.perform(post("/tools").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"   \"}")
                        .header(AUTHORIZATION, owner))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        MvcResult created = mvc.perform(post("/tools").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Macaco\",\"description\":\"Original\"}")
                        .header(AUTHORIZATION, owner))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();
        long id = json.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(get("/tools").header(AUTHORIZATION, other))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
        mvc.perform(get("/tools/{id}", id).header(AUTHORIZATION, other))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("TOOL_ACCESS_DENIED"));
        mvc.perform(put("/tools/{id}", id).header(AUTHORIZATION, other)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Roubado\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/tools/{id}/deactivate", id).header(AUTHORIZATION, other))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/tools/{id}", id).header(AUTHORIZATION, other))
                .andExpect(status().isForbidden());

        mvc.perform(put("/tools/{id}", id).header(AUTHORIZATION, owner)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"description\":\"  Nova  \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Macaco"))
                .andExpect(jsonPath("$.description").value("Nova"));
        mvc.perform(patch("/tools/{id}/deactivate", id).header(AUTHORIZATION, owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));
        mvc.perform(get("/tools/{id}", id).header(AUTHORIZATION, owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));
        mvc.perform(delete("/tools/{id}", id).header(AUTHORIZATION, owner))
                .andExpect(status().isNoContent());
        assertThat(history.findAll()).anySatisfy(snapshot -> {
            assertThat(snapshot.getToolId()).isEqualTo(id);
            assertThat(snapshot.getStatus().name()).isEqualTo("INACTIVE");
        });
        mvc.perform(get("/tools/{id}", id).header(AUTHORIZATION, owner))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("TOOL_NOT_FOUND"));
    }

    @Test
    @Transactional
    void adminRequiresPermissionAndCanManageOtherOwnersTools() throws Exception {
        String member = token("Tools Member", "62847195319");
        String admin = token("Tools Manager", "73928461591");
        Long memberId = users.findByCpf("62847195319").orElseThrow().id();
        Long adminId = users.findByCpf("73928461591").orElseThrow().id();
        mvc.perform(get("/admin/tools").header(AUTHORIZATION, admin))
                .andExpect(status().isForbidden());

        Instant now = Instant.now(clock);
        Role role = roles.save(Role.create("tools-suite-manager", "Tools suite", now));
        userRoles.save(UserRole.create(adminId, role.getId(), now));
        for (PermissionCode code : new PermissionCode[]{PermissionCode.TOOLS_TOOL_READ,
                PermissionCode.TOOLS_TOOL_CREATE, PermissionCode.TOOLS_TOOL_UPDATE,
                PermissionCode.TOOLS_TOOL_ACTIVATE, PermissionCode.TOOLS_TOOL_DEACTIVATE,
                PermissionCode.TOOLS_TOOL_DELETE}) {
            rolePermissions.save(RolePermission.create(role.getId(),
                    permissions.findByCode(code).orElseThrow().getId(), now));
        }

        MvcResult created = mvc.perform(post("/admin/tools/users/{userId}", memberId)
                        .header(AUTHORIZATION, admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Chave\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.userId").value(memberId))
                .andReturn();
        long id = json.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/admin/tools/{id}", id).header(AUTHORIZATION, admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Chave"));
        mvc.perform(patch("/admin/tools/{id}/deactivate", id).header(AUTHORIZATION, admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));
        mvc.perform(get("/admin/tools").param("status", "INACTIVE").param("name", "cha")
                        .header(AUTHORIZATION, admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(id));
        mvc.perform(get("/tools/{id}", id).header(AUTHORIZATION, member))
                .andExpect(status().isOk());
        mvc.perform(delete("/admin/tools/{id}", id).header(AUTHORIZATION, admin))
                .andExpect(status().isNoContent());
        mvc.perform(get("/admin/tools/{id}", id).header(AUTHORIZATION, admin))
                .andExpect(status().isNotFound());
    }

    private String token(String name, String cpf) {
        return "Bearer " + registration.registerAndAuthenticate(new UserRegistrationData(
                name, null, null, cpf, null, null, null, Instant.now(clock)),
                "tools-suite-password").accessToken();
    }
}
