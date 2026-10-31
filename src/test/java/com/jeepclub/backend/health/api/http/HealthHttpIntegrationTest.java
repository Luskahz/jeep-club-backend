package com.jeepclub.backend.health.api.http;

import com.jeepclub.backend.dependents.core.domain.enums.DependentStatus;
import com.jeepclub.backend.dependents.api.module.DependentsQuery;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
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
        "spring.jpa.properties.hibernate.generate_statistics=true", "logging.level.org.hibernate.stat=OFF",
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
    @MockitoSpyBean UserQuery userQuery;
    @MockitoSpyBean DependentsQuery dependentsQuery;
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
        em.clear();
        assertThat(profiles.findById(profile.getId()).orElseThrow().getAllergies()).isEqualTo(SENTINEL);
        assertThat(profiles.findByOwner(MedicalProfileOwnerType.DEPENDENT, dependent.getId())
                .orElseThrow().getAllergies()).isEqualTo(SENTINEL);
        assertThat(history.count()).isZero();
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
        var saved = seed(MedicalProfileOwnerType.DEPENDENT, dependent.getId());
        dependent.setStatus(DependentStatus.INACTIVE);
        em.flush();
        problem(call(method, "/medical-profiles/dependents/" + dependent.getId(), "{}"), 409)
                .andExpect(jsonPath("$.code").value("MEDICAL_PROFILE_OWNER_INACTIVE"));
        problem(call(method, "/medical-profiles/dependents/999999999", "{}"), 404)
                .andExpect(jsonPath("$.code").value("MEDICAL_PROFILE_OWNER_NOT_FOUND"));
        problem(call(method, "/medical-profiles/dependents/0", "{}"), 400);
        em.clear();
        assertThat(profiles.findById(saved.getId()).orElseThrow().getAllergies()).isEqualTo(SENTINEL);
        assertThat(history.count()).isZero();
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
        other.setStatus(UserStatus.INACTIVE);
        other.setDisabledAt(CREATED.plusSeconds(1));
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

    @Test
    @org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
    void allAdministrativeReadRoutesReturnCurrentNamesAndNoAdditionalRegistrationData(org.springframework.boot.test.system.CapturedOutput output) throws Exception {
        user.setName("Responsible A"); other.setName("Responsible B");
        dependent.setName("Same child"); foreignDependent.setName("Same child");
        var own = seed(MedicalProfileOwnerType.USER, user.getId());
        var child = seed(MedicalProfileOwnerType.DEPENDENT, dependent.getId());
        seed(MedicalProfileOwnerType.DEPENDENT, foreignDependent.getId());
        em.flush(); em.clear();
        permissions("HEALTH_MEDICAL_PROFILE_READ");
        for (String path : List.of("/admin/medical-profiles/" + own.getId(), "/admin/medical-profiles/users/" + user.getId())) {
            call("GET", path, null).andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(jsonPath("$.ownerName").value("Responsible A"))
                    .andExpect(jsonPath("$.responsibleName").value(org.hamcrest.Matchers.nullValue()))
                    .andExpect(jsonPath("$.ownerId").value(user.getId()))
                    .andExpect(jsonPath("$.allergies").value(SENTINEL))
                    .andExpect(jsonPath("$.cpf").doesNotExist()).andExpect(jsonPath("$.email").doesNotExist())
                    .andExpect(jsonPath("$.phoneNumber").doesNotExist());
        }
        for (String path : List.of("/admin/medical-profiles/" + child.getId(), "/admin/medical-profiles/dependents/" + dependent.getId())) {
            call("GET", path, null).andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(jsonPath("$.ownerName").value("Same child"))
                    .andExpect(jsonPath("$.responsibleName").value("Responsible A"))
                    .andExpect(jsonPath("$.ownerId").value(dependent.getId()))
                    .andExpect(jsonPath("$.cpf").doesNotExist()).andExpect(jsonPath("$.userId").doesNotExist())
                    .andExpect(jsonPath("$.email").doesNotExist()).andExpect(jsonPath("$.phoneNumber").doesNotExist());
        }
        var response = call("GET", "/admin/medical-profiles?sort=id,asc", null).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.content[0].ownerName").value("Responsible A"))
                .andExpect(jsonPath("$.content[1].responsibleName").value("Responsible A"))
                .andExpect(jsonPath("$.content[2].responsibleName").value("Responsible B"))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("cpf", "email", "phoneNumber", "birthDate", "relationshipType", SENTINEL);
        assertThat(output.getAll()).doesNotContain(SENTINEL);
        // Personal JSON stays unchanged despite the administrative DTO extension.
        call("GET", "/medical-profiles/me", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerName").doesNotExist()).andExpect(jsonPath("$.responsibleName").doesNotExist());
    }

    @Test
    void nextReadReflectsRenamedUserDependentAndChangedResponsible() throws Exception {
        var own = seed(MedicalProfileOwnerType.USER, user.getId());
        var child = seed(MedicalProfileOwnerType.DEPENDENT, dependent.getId());
        permissions("HEALTH_MEDICAL_PROFILE_READ");
        call("GET", "/admin/medical-profiles/" + own.getId(), null).andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerName").value("Synthetic user"));
        user.setName("Renamed user"); dependent.setName("Renamed child"); other.setName("New responsible");
        dependent.setUserId(other.getId());
        em.flush(); em.clear();
        call("GET", "/admin/medical-profiles/" + own.getId(), null).andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerName").value("Renamed user"));
        call("GET", "/admin/medical-profiles/dependents/" + dependent.getId(), null).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(child.getId()))
                .andExpect(jsonPath("$.ownerName").value("Renamed child"))
                .andExpect(jsonPath("$.responsibleName").value("New responsible"));
    }

    @Test
    void fiftyProfilesUseTwoPresentationBatchesAndKeepPaginationSortingAndTotals() throws Exception {
        seed(MedicalProfileOwnerType.USER, user.getId());
        for (int i = 1; i <= 50; i++) {
            var child = dependent(user.getId(), String.format("8000000%04d", i));
            seed(MedicalProfileOwnerType.DEPENDENT, child.getId());
        }
        var excluded = seed(MedicalProfileOwnerType.USER, other.getId());
        other.setStatus(UserStatus.INACTIVE);
        em.flush(); em.clear();
        permissions("HEALTH_MEDICAL_PROFILE_READ");
        clearInvocations(userQuery, dependentsQuery);
        var statistics = em.getEntityManagerFactory().unwrap(org.hibernate.SessionFactory.class).getStatistics();
        long before = statistics.getPrepareStatementCount();
        var page = call("GET", "/admin/medical-profiles?size=999&sort=id,desc", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50)).andExpect(jsonPath("$.content.length()").value(50))
                .andExpect(jsonPath("$.totalElements").value(51)).andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].ownerType").value("DEPENDENT"))
                .andExpect(jsonPath("$.content[49].responsibleName").value("Synthetic user"))
                .andReturn().getResponse().getContentAsString();
        assertThat(statistics.getPrepareStatementCount() - before).as("SQL statements for a 50-profile page").isLessThanOrEqualTo(6);
        var content = json.readTree(page).get("content");
        long previous = Long.MAX_VALUE;
        for (var item : content) {
            assertThat(item.get("id").asLong()).isLessThan(previous).isNotEqualTo(excluded.getId());
            previous = item.get("id").asLong();
        }
        verify(dependentsQuery).findDetailsByIds(argThat(ids -> ids.size() == 50));
        verify(userQuery).findByIds(argThat(ids -> ids.size() == 1 && ids.contains(user.getId())));
        verify(userQuery, never()).findById(any());
        verify(dependentsQuery, never()).existsById(any());
        call("GET", "/admin/medical-profiles?page=1&size=50&sort=id,desc", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(51)).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].ownerType").value("USER"))
                .andExpect(jsonPath("$.content[0].responsibleName").value(org.hamcrest.Matchers.nullValue()));
        clearInvocations(userQuery, dependentsQuery);
        call("GET", "/admin/medical-profiles?page=2&size=50", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(51)).andExpect(jsonPath("$.empty").value(true));
        verify(userQuery, never()).findByIds(any()); verify(dependentsQuery, never()).findDetailsByIds(any());
    }

    @Test
    void emptyPageDoesNotResolveNames() throws Exception {
        permissions("HEALTH_MEDICAL_PROFILE_READ");
        clearInvocations(userQuery, dependentsQuery);
        call("GET", "/admin/medical-profiles", null).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.totalElements").value(0)).andExpect(jsonPath("$.empty").value(true));
        verify(userQuery, never()).findByIds(any()); verify(dependentsQuery, never()).findDetailsByIds(any());
    }

    @Test
    void brokenResponsibleRegistrationDoesNotReturnPartiallyIdentifiedClinicalData() throws Exception {
        var saved = seed(MedicalProfileOwnerType.DEPENDENT, dependent.getId());
        em.flush(); em.clear();
        // Simulate an inconsistent upstream projection while physical references remain valid.
        doReturn(List.of()).when(userQuery).findByIds(any());
        permissions("HEALTH_MEDICAL_PROFILE_READ");
        for (String path : List.of("/admin/medical-profiles/" + saved.getId(),
                "/admin/medical-profiles/dependents/" + dependent.getId(), "/admin/medical-profiles")) {
            problem(call("GET", path, null), 404).andExpect(jsonPath("$.code").value("MEDICAL_PROFILE_OWNER_NOT_FOUND"));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void administrativeOwnerAndProfileSelectorsPreserveMissingAndInactiveErrors(boolean forDependent) throws Exception {
        var type = forDependent ? MedicalProfileOwnerType.DEPENDENT : MedicalProfileOwnerType.USER;
        var ownerId = forDependent ? dependent.getId() : user.getId();
        String ownerPath = "/admin/medical-profiles/" + (forDependent ? "dependents/" : "users/") + ownerId;
        permissions("HEALTH_MEDICAL_PROFILE_READ");
        problem(call("GET", ownerPath, null), 404).andExpect(jsonPath("$.code").value("MEDICAL_PROFILE_NOT_FOUND"));
        var saved = seed(type, ownerId);
        if (forDependent) dependent.setStatus(DependentStatus.INACTIVE); else user.setStatus(UserStatus.INACTIVE);
        em.flush(); em.clear();
        for (String path : List.of(ownerPath, "/admin/medical-profiles/" + saved.getId())) {
            problem(call("GET", path, null), 409).andExpect(jsonPath("$.code").value("MEDICAL_PROFILE_OWNER_INACTIVE"));
        }
        if (forDependent) doReturn(false).when(dependentsQuery).existsById(ownerId);
        else doReturn(false).when(userQuery).existsById(ownerId);
        problem(call("GET", "/admin/medical-profiles/" + saved.getId(), null), 404)
                .andExpect(jsonPath("$.code").value("MEDICAL_PROFILE_OWNER_NOT_FOUND"));
        problem(call("GET", "/admin/medical-profiles/999999999", null), 404)
                .andExpect(jsonPath("$.code").value("MEDICAL_PROFILE_NOT_FOUND"));
    }

    @Test
    void healthExportsRejectInvalidSelectorsAndMissingOwnersBeforeReturningClinicalData() throws Exception {
        permissions("HEALTH_MEDICAL_PROFILE_EXPORT");
        String path = "/admin/medical-profiles/export?";
        for (String filter : List.of("profileId=0", "userId=-1", "dependentId=0", "household=true",
                "profileId=1&userId=" + user.getId(), "userId=" + user.getId() + "&dependentId=" + dependent.getId())) {
            problem(call("GET", path + filter, null), 400);
        }
        problem(call("GET", path + "userId=999999999", null), 404);
        problem(call("GET", path + "dependentId=999999999", null), 404);
        assertThat(history.count()).isZero();
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
