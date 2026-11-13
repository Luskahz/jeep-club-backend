package com.jeepclub.backend.dependents.api.controller;

import com.jeepclub.backend.dependents.core.domain.enums.DependentStatus;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.infra.persistence.entity.DependentEntity;
import com.jeepclub.backend.iam.authentication.core.application.service.security.AccessTokenAuthenticationService;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import com.jeepclub.backend.iam.identity.api.module.UserStatus;
import com.jeepclub.backend.iam.identity.infra.persistence.entity.UserEntity;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.platform.security.jwt.JwtAuthenticatedUser;
import com.jeepclub.backend.platform.security.jwt.JwtTokenParser;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.jeepclub.backend.dependents.api.controller.AdminDependentSearchHttpIntegrationTest$Sql")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminDependentSearchHttpIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    EntityManager em;
    @Autowired
    UserQuery identities;
    @MockitoBean
    JwtTokenParser jwt;
    @MockitoBean
    UserAuthoritiesProvider authorities;
    @MockitoBean
    AccessTokenAuthenticationService authentication;

    @BeforeEach
    void auth() {
        when(jwt.parseAndValidate("search-token"))
                .thenReturn(new JwtAuthenticatedUser(99L, 199L, "Admin", Instant.now().plusSeconds(3600)));
        when(authorities.findAuthorityCodesByUserId(99L))
                .thenReturn(List.of("DEPENDENTS_DEPENDENT_READ"));
    }

    @Test
    void ownerNameIsAppliedBeforePageCountAndSelectedFieldsDoNotLeakAuxiliaryIdOrStorageKey() throws Exception {
        long owner = owner("Literal %_! Titular");
        var first = dependent(owner, "Ana Dependent", "11122233344", RelationshipType.CHILD, "11999990001", "private/dep-1.jpg");
        var second = dependent(owner, "Bruno Dependent", "22233344455", RelationshipType.SPOUSE, "11999990002", null);
        dependent(owner("Outro Titular"), "Carlos Dependent", "33344455566", RelationshipType.CHILD, "11999990003", null);

        mvc.perform(get("/admin/dependents")
                        .header("Authorization", "Bearer search-token")
                        .param("q", " %_! ")
                        .param("name", "dependent")
                        .param("size", "1")
                        .param("page", "1")
                        .param("sort", "name,asc")
                        .param("fields", "ID,OWNER_NAME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].id").value(second.getId()))
                .andExpect(jsonPath("$.content[0].ownerName").value("Literal %_! Titular"))
                .andExpect(jsonPath("$.content[0].userId").doesNotExist())
                .andExpect(jsonPath("$.content[0].cpf").doesNotExist())
                .andExpect(jsonPath("$.content[0].photoStorageKey").doesNotExist());

        mvc.perform(get("/admin/dependents")
                        .header("Authorization", "Bearer search-token")
                        .param("userId", Long.toString(owner))
                        .param("fields", "PHOTO_URL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].photoUrl").value("/users/" + owner + "/dependents/" + first.getId() + "/photo"))
                .andExpect(jsonPath("$.content[0].id").doesNotExist())
                .andExpect(jsonPath("$.content[0].userId").doesNotExist())
                .andExpect(jsonPath("$.content[0].photoStorageKey").doesNotExist());

        mvc.perform(get("/admin/dependents/" + first.getId())
                        .header("Authorization", "Bearer search-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(first.getId()))
                .andExpect(jsonPath("$.ownerName").value("Literal %_! Titular"))
                .andExpect(jsonPath("$.photoUrl").value("/users/" + owner + "/dependents/" + first.getId() + "/photo"))
                .andExpect(jsonPath("$.photoStorageKey").doesNotExist());

        assertThat(identities.findIdsByName(" %_! ")).containsExactly(owner);
    }

    @Test
    void qUsesOrWithConditionalNumericMatchingAndAlphanumericDoesNotMatchCpfOrPhone() throws Exception {
        long owner = owner("Roberto Titular");
        var row = dependent(owner, "Maria !%_ Silva", "52998224725", RelationshipType.SPOUSE, "11988887777", null);
        var numericOther = dependent(owner("Outro Socio"), "Lucas Costa", "99900012345", RelationshipType.CHILD, "11912345678", null);

        for (String q : List.of("529.982.247-25", "(11) 98888-7777", "!%_", "maria !%_", "Roberto Titular")) {
            mvc.perform(get("/admin/dependents")
                            .header("Authorization", "Bearer search-token")
                            .param("q", q))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(row.getId()));
        }

        // Alphanumeric term "maria123" must NOT strip letters and match numericOther's CPF/phone containing "123"
        mvc.perform(get("/admin/dependents")
                        .header("Authorization", "Bearer search-token")
                        .param("q", "maria123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        // Formatted CPF filter and relationshipType filter combine with AND
        mvc.perform(get("/admin/dependents")
                        .header("Authorization", "Bearer search-token")
                        .param("cpf", " 529.982.247-25 ")
                        .param("relationshipType", "SPOUSE")
                        .param("userId", Long.toString(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(row.getId()));

        numericOther.setStatus(DependentStatus.ARCHIVED);
        em.flush();

        mvc.perform(get("/admin/dependents")
                        .header("Authorization", "Bearer search-token")
                        .param("status", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(numericOther.getId()));
    }

    @Test
    void broadOwnerMatchesPreserveEveryOrBranchAndDatabasePagination() throws Exception {
        DependentEntity lastOwnerMatch = null;
        for (int i = 0; i < 501; i++) {
            long id = owner("XYZ Broad Dependent Owner");
            if (i == 500) {
                em.find(UserEntity.class, id).setStatus(UserStatus.ARCHIVED);
            }
            lastOwnerMatch = dependent(
                    id,
                    i == 500 ? "XYZ dependent" : "Regular Name",
                    String.format("8%010d", i),
                    RelationshipType.CHILD,
                    "11900001111",
                    null
            );
        }
        long other = owner("Unrelated Owner");
        var byName = dependent(other, "XYZ child name", "70000000001", RelationshipType.CHILD, "11900002222", null);
        em.flush();
        em.clear();
        Sql.statements.get().clear();

        mvc.perform(get("/admin/dependents")
                        .header("Authorization", "Bearer search-token")
                        .param("q", "xyz")
                        .param("page", "10")
                        .param("size", "50")
                        .param("fields", "ID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(502))
                .andExpect(jsonPath("$.totalPages").value(11))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(lastOwnerMatch.getId()))
                .andExpect(jsonPath("$.content[1].id").value(byName.getId()))
                .andExpect(jsonPath("$.content[0].userId").doesNotExist())
                .andExpect(jsonPath("$.content[0].ownerName").doesNotExist());

        assertThat(Sql.statements.get()).hasSize(2).allSatisfy(sql ->
                assertThat(sql).contains("exists", "user_id"));
        String contentSql = Sql.statements.get().get(0);
        assertThat(contentSql.substring(0, contentSql.indexOf(" from ")))
                .contains(".id")
                .doesNotContain(".user_id", ".name", ".cpf", ".photo_storage_key");
        assertThat(contentSql).containsAnyOf("offset ? rows fetch first ? rows only", "limit ?,?");
    }

    @Test
    void invalidParametersAre400AndAuthorizationIsPreserved() throws Exception {
        Map<String, String> invalidParams = Map.of(
                "fields", "PHOTO_STORAGE_KEY",
                "sort", "ownerName,asc",
                "userId", "0",
                "status", "INVALID",
                "relationshipType", "INVALID",
                "q", "x".repeat(151),
                "page", "-1",
                "size", "0",
                "cpf", "---"
        );
        for (var invalid : invalidParams.entrySet()) {
            mvc.perform(get("/admin/dependents")
                            .header("Authorization", "Bearer search-token")
                            .param(invalid.getKey(), invalid.getValue()))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        }

        mvc.perform(get("/admin/dependents")
                        .header("Authorization", "Bearer search-token")
                        .param("cpf", "abc123"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));

        mvc.perform(get("/admin/dependents")
                        .header("Authorization", "Bearer search-token")
                        .param("page", Integer.toString(Integer.MAX_VALUE)))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/admin/dependents/0")
                        .header("Authorization", "Bearer search-token"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/admin/dependents/999999")
                        .header("Authorization", "Bearer search-token"))
                .andExpect(status().isNotFound());

        mvc.perform(get("/admin/dependents")).andExpect(status().isUnauthorized());

        when(authorities.findAuthorityCodesByUserId(99L)).thenReturn(List.of());
        mvc.perform(get("/admin/dependents")
                        .header("Authorization", "Bearer search-token"))
                .andExpect(status().isForbidden());
    }

    private long owner(String name) {
        var row = new UserEntity();
        row.setName(name);
        row.setCpf(String.format("%011d", Math.abs(UUID.randomUUID().getMostSignificantBits() % 100000000000L)));
        row.setStatus(UserStatus.ACTIVE);
        row.setCreatedAt(Instant.now());
        em.persist(row);
        return row.getId();
    }

    private DependentEntity dependent(
            long userId,
            String name,
            String cpf,
            RelationshipType relationshipType,
            String phoneNumber,
            String photoStorageKey
    ) {
        var row = new DependentEntity();
        row.setUserId(userId);
        row.setName(name);
        row.setCpf(cpf);
        row.setBirthDate(LocalDate.of(2012, 4, 15));
        row.setRelationshipType(relationshipType);
        row.setPhoneNumber(phoneNumber);
        row.setPhotoStorageKey(photoStorageKey);
        row.setStatus(DependentStatus.ACTIVE);
        row.setCreatedAt(Instant.parse("2026-06-30T12:00:00Z"));
        em.persist(row);
        em.flush();
        return row;
    }

    public static class Sql implements org.hibernate.resource.jdbc.spi.StatementInspector {
        static final ThreadLocal<List<String>> statements = ThreadLocal.withInitial(ArrayList::new);

        @Override
        public String inspect(String sql) {
            if (sql.startsWith("select ")) {
                statements.get().add(sql);
            }
            return sql;
        }
    }
}
