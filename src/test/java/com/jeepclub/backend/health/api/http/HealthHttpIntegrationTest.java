package com.jeepclub.backend.health.api.http;

import com.jeepclub.backend.dependents.core.domain.enums.DependentStatus;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.infra.persistence.entity.DependentEntity;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import com.jeepclub.backend.health.core.repository.MedicalProfileRepository;
import com.jeepclub.backend.health.infra.persistence.jpa.MedicalProfileHistoryJpaRepository;
import com.jeepclub.backend.iam.authentication.core.application.service.security.AccessTokenAuthenticationService;
import com.jeepclub.backend.iam.identity.api.module.UserStatus;
import com.jeepclub.backend.iam.identity.infra.persistence.entity.UserEntity;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.platform.security.jwt.JwtAuthenticatedUser;
import com.jeepclub.backend.platform.security.jwt.JwtTokenParser;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:health_http_audit;DB_CLOSE_DELAY=-1;MODE=MySQL;NON_KEYWORDS=USER",
        "spring.jpa.show-sql=false", "logging.level.org.hibernate.SQL=OFF", "logging.level.org.hibernate.orm.jdbc.bind=OFF"})
@AutoConfigureMockMvc(printOnlyOnFailure = false, print = org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint.NONE)
@ActiveProfiles("test")
@Transactional
class HealthHttpIntegrationTest {
    private static final Instant CREATED = Instant.parse("2026-09-20T12:00:00Z");
    private static final String SENTINEL = "SYNTHETIC_HEALTH_MARKER";
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired MedicalProfileRepository profiles;
    @Autowired MedicalProfileHistoryJpaRepository history;
    @Autowired JsonMapper json;
    // Only the authentication boundary is stubbed; filters, method security, ownership, services and JPA are real.
    @MockitoBean JwtTokenParser parser;
    @MockitoBean UserAuthoritiesProvider authorities;
    @MockitoBean AccessTokenAuthenticationService authentication;
    private UserEntity user;
    private UserEntity other;
    private DependentEntity dependent;
    private DependentEntity foreignDependent;

    @BeforeEach
    void fixtures() {
        user = user("90000000001");
        other = user("90000000002");
        dependent = dependent(user.getId(), "90000000003");
        foreignDependent = dependent(other.getId(), "90000000004");
        when(parser.parseAndValidate("health-audit")).thenReturn(new JwtAuthenticatedUser(user.getId(), 1L,
                "Synthetic actor", Instant.parse("2099-01-01T00:00:00Z")));
        permissions();
    }

    static Stream<Arguments> routes() {
        return Stream.of(
                Arguments.of("GET", "/medical-profiles/me", ""),
                Arguments.of("PUT", "/medical-profiles/me", ""),
                Arguments.of("DELETE", "/medical-profiles/me", ""),
                Arguments.of("GET", "/medical-profiles/dependents/{dependent}", ""),
                Arguments.of("PUT", "/medical-profiles/dependents/{dependent}", ""),
                Arguments.of("DELETE", "/medical-profiles/dependents/{dependent}", ""),
                Arguments.of("GET", "/admin/medical-profiles", "READ"),
                Arguments.of("GET", "/admin/medical-profiles/{profile}", "READ"),
                Arguments.of("GET", "/admin/medical-profiles/users/{user}", "READ"),
                Arguments.of("PUT", "/admin/medical-profiles/users/{user}", "UPDATE"),
                Arguments.of("GET", "/admin/medical-profiles/dependents/{dependent}", "READ"),
                Arguments.of("PUT", "/admin/medical-profiles/dependents/{dependent}", "UPDATE"),
                Arguments.of("DELETE", "/admin/medical-profiles/{profile}", "DELETE"));
    }

    static Stream<Arguments> adminRoutes() { return routes().filter(a -> !a.get()[2].equals("")); }

    @ParameterizedTest
    @MethodSource("routes")
    void everyOperationalRouteRequiresAuthentication(String method, String path, String permission) throws Exception {
        problem(mvc.perform(request(HttpMethod.valueOf(method), resolve(path, 1L))
                .contentType(MediaType.APPLICATION_JSON).content("{}")), 401);
    }

    @ParameterizedTest
    @MethodSource("adminRoutes")
    void administrativeRoutesRequireTheirSpecificAuthority(String method, String path, String permission) throws Exception {
        var profile = seed(MedicalProfileOwnerType.USER, user.getId());
        seed(MedicalProfileOwnerType.DEPENDENT, dependent.getId());
        String route = resolve(path, profile.getId());
        // An authenticated actor with every OTHER health authority still cannot use this operation.
        permissions(Stream.of("READ", "UPDATE", "DELETE", "EXPORT").filter(p -> !p.equals(permission))
                .map(p -> "HEALTH_MEDICAL_PROFILE_" + p).toArray(String[]::new));
        problem(call(method, route, "{}"), 403);
        permissions("HEALTH_MEDICAL_PROFILE_" + permission);
        call(method, route, "{}").andExpect(status().is(method.equals("DELETE") ? 204 : 200));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void memberCanCreateReplaceReadAndDeleteOnlyItsOwnResource(boolean forDependent) throws Exception {
        String path = forDependent ? "/medical-profiles/dependents/" + dependent.getId() : "/medical-profiles/me";
        Long ownerId = forDependent ? dependent.getId() : user.getId();
        var ownerType = forDependent ? MedicalProfileOwnerType.DEPENDENT : MedicalProfileOwnerType.USER;
        problem(call("GET", path, null), 404);
        String body = "{\"allergies\":\"  " + SENTINEL + "  \",\"emergencyContactPhone\":\"(00) 00000-0000\"}";
        var response = call("PUT", path + "?userId=" + other.getId(), body).andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerType").value(ownerType.name()))
                .andExpect(jsonPath("$.ownerId").value(ownerId))
                .andExpect(jsonPath("$.allergies").value(SENTINEL))
                .andExpect(jsonPath("$.emergencyContactPhone").value("00000000000"))
                .andReturn().getResponse();
        long profileId = json.readTree(response.getContentAsString()).get("id").asLong();
        em.clear();
        call("GET", path, null).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(profileId));
        call("PUT", path, "{}").andExpect(status().isOk()).andExpect(jsonPath("$.id").value(profileId))
                .andExpect(jsonPath("$.allergies").isEmpty()).andExpect(jsonPath("$.bloodType").value("UNKNOWN"));
        assertThat(profiles.existsByOwner(MedicalProfileOwnerType.USER, other.getId())).isFalse();
        call("DELETE", path, null).andExpect(status().isNoContent()).andExpect(content().string(""));
        em.clear();
        problem(call("GET", path, null), 404);
        assertThat(history.findAll()).singleElement().satisfies(row -> {
            assertThat(row.getMedicalProfileId()).isEqualTo(profileId);
            assertThat(row.getDeletedByUserId()).isEqualTo(user.getId());
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "DELETE"})
    void foreignDependentCannotBeReadChangedOrDeleted(String method) throws Exception {
        var saved = seed(MedicalProfileOwnerType.DEPENDENT, foreignDependent.getId());
        problem(call(method, "/medical-profiles/dependents/" + foreignDependent.getId(), "{}"), 403)
                .andExpect(jsonPath("$.code").value("MEDICAL_PROFILE_ACCESS_DENIED"));
        em.clear();
        assertThat(profiles.findById(saved.getId()).orElseThrow().getAllergies()).isEqualTo(SENTINEL);
        assertThat(history.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "DELETE"})
    void inactiveAndMissingDependentsHaveControlledErrors(String method) throws Exception {
        dependent.setStatus(DependentStatus.DISABLED);
        em.flush();
        problem(call(method, "/medical-profiles/dependents/" + dependent.getId(), "{}"), 409)
                .andExpect(jsonPath("$.code").value("MEDICAL_PROFILE_OWNER_INACTIVE"));
        problem(call(method, "/medical-profiles/dependents/999999999", "{}"), 404)
                .andExpect(jsonPath("$.code").value("MEDICAL_PROFILE_OWNER_NOT_FOUND"));
        problem(call(method, "/medical-profiles/dependents/0", "{}"), 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "null", "{\"bloodType\":\"INVALID\"}", "{\"emergencyContactPhone\":\"123\"}"})
    void invalidHttpInputUsesProblemDetailsAndDoesNotPersist(String body) throws Exception {
        permissions("HEALTH_MEDICAL_PROFILE_UPDATE");
        for (String path : List.of("/medical-profiles/me", "/admin/medical-profiles/users/" + user.getId())) {
            problem(call("PUT", path, body), 400);
        }
        assertThat(profiles.existsByOwner(MedicalProfileOwnerType.USER, user.getId())).isFalse();
    }

    @Test
    void validationDoesNotEchoClinicalInputOrModifyExistingData() throws Exception {
        var saved = seed(MedicalProfileOwnerType.USER, user.getId());
        problem(call("PUT", "/medical-profiles/me", "{\"observations\":\"" + SENTINEL.repeat(100) + "\"}"), 400);
        em.clear();
        assertThat(profiles.findById(saved.getId()).orElseThrow().getAllergies()).isEqualTo(SENTINEL);
    }

    @Test
    void adminSummaryPaginationAndMutationDoNotExposeClinicalContentAndInactiveOwnerCanBeCleaned() throws Exception {
        seed(MedicalProfileOwnerType.USER, user.getId());
        var inactive = seed(MedicalProfileOwnerType.USER, other.getId());
        other.setStatus(UserStatus.DISABLED);
        seed(MedicalProfileOwnerType.DEPENDENT, dependent.getId());
        em.flush();
        permissions("HEALTH_MEDICAL_PROFILE_READ", "HEALTH_MEDICAL_PROFILE_UPDATE", "HEALTH_MEDICAL_PROFILE_DELETE");
        call("GET", "/admin/medical-profiles?page=1&size=1&sort=id,asc", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2)).andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(1)).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].ownerType").value("DEPENDENT"))
                .andExpect(jsonPath("$.content[0].allergies").doesNotExist())
                .andExpect(jsonPath("$.pageable").doesNotExist()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(SENTINEL))));
        call("GET", "/admin/medical-profiles?size=999", null).andExpect(status().isOk()).andExpect(jsonPath("$.size").value(50));
        call("PUT", "/admin/medical-profiles/users/" + user.getId(), "{\"allergies\":\"" + SENTINEL + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.allergies").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(SENTINEL))));
        problem(call("GET", "/admin/medical-profiles/" + inactive.getId(), null), 409);
        call("DELETE", "/admin/medical-profiles/" + inactive.getId(), null).andExpect(status().isNoContent());
        assertThat(profiles.findById(inactive.getId())).isEmpty();
    }

    private ResultActions problem(ResultActions result, int status) throws Exception {
        return result.andExpect(status().is(status)).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.title").isNotEmpty()).andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.code").isNotEmpty()).andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(SENTINEL))));
    }

    private ResultActions call(String method, String path, String body) throws Exception {
        var request = request(HttpMethod.valueOf(method), path).header("Authorization", "Bearer health-audit");
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(request);
    }

    private void permissions(String... permissions) { when(authorities.findAuthorityCodesByUserId(user.getId())).thenReturn(List.of(permissions)); }
    private String resolve(String path, Long profile) {
        return path.replace("{user}", user.getId().toString()).replace("{dependent}", dependent.getId().toString())
                .replace("{profile}", profile.toString());
    }
    private MedicalProfile seed(MedicalProfileOwnerType type, Long owner) {
        return profiles.save(MedicalProfile.create(type, owner, null, SENTINEL,
                null, null, null, null, null, null, null, null, null, CREATED));
    }
    private UserEntity user(String cpf) {
        var entity = new UserEntity();
        entity.setName("Synthetic user"); entity.setCpf(cpf); entity.setStatus(UserStatus.ACTIVE); entity.setCreatedAt(CREATED);
        em.persist(entity); em.flush(); return entity;
    }
    private DependentEntity dependent(Long userId, String cpf) {
        var entity = new DependentEntity();
        entity.setName("Synthetic dependent"); entity.setCpf(cpf); entity.setBirthDate(LocalDate.of(2010, 1, 1));
        entity.setRelationshipType(RelationshipType.CHILD); entity.setUserId(userId);
        entity.setStatus(DependentStatus.ACTIVE); entity.setCreatedAt(CREATED);
        em.persist(entity); em.flush(); return entity;
    }
}
