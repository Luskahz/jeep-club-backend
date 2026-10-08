package com.jeepclub.backend.identity.api.controller;

import com.jeepclub.backend.iam.identity.api.module.UserDetails;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import com.jeepclub.backend.iam.identity.api.module.UserRegistration;
import com.jeepclub.backend.iam.identity.api.module.UserRegistrationData;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.stream.Stream;
import java.util.Map;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:identity_profile_test;DB_CLOSE_DELAY=-1;MODE=MySQL;NON_KEYWORDS=USER")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CurrentUserProfileIntegrationTest {
    @TempDir static Path mediaRoot;
    @DynamicPropertySource
    static void storage(DynamicPropertyRegistry registry) {
        registry.add("storage.local.root-directory", () -> mediaRoot.toString());
    }

    @Autowired MockMvc mvc;
    @Autowired UserRegistration registration;
    @Autowired UserQuery users;
    @Autowired EntityManager entityManager;
    @Autowired Clock clock;
    @Autowired JsonMapper json;
    private String token;
    private UserDetails before;
    private Long otherId;

    @BeforeEach
    void setUp() {
        Instant now = clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        token = registration.registerAndAuthenticate(new UserRegistrationData("Original", LocalDate.of(2000, 5, 17),
                "original@example.com", "52998224725", "123456789", "5511999999999", null, now),
                "profile-password").accessToken();
        before = users.findByCpf("52998224725").orElseThrow();
        otherId = registration.createWithPermanentCredential(new UserRegistrationData("Other", null,
                "other@example.com", "16899535009", "987654321", null, null, now), "other-password");
    }

    @Test
    void updatesOnlyTheAuthenticatedIdentityAndReturnsCanonicalFields() throws Exception {
        mvc.perform(patch("/identity/me").queryParam("userId", otherId.toString())
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"  Maria Silva  ","birthDate":"1999-12-31",
                                 "rg":"22.333.444-5","phoneNumber":"+55 (11) 98888-7777"}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(before.id()))
                .andExpect(jsonPath("$.name").value("Maria Silva"))
                .andExpect(jsonPath("$.rg").value("223334445"))
                .andExpect(jsonPath("$.phoneNumber").value("5511988887777"))
                .andExpect(jsonPath("$.birthDate").value("1999-12-31"))
                .andExpect(jsonPath("$.cpf").value(before.cpf()))
                .andExpect(jsonPath("$.email").value(before.email()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        entityManager.clear();
        UserDetails saved = users.findById(before.id()).orElseThrow();
        assertThat(saved.createdAt()).isEqualTo(before.createdAt());
        assertThat(saved.updatedAt()).isAfterOrEqualTo(saved.createdAt());
        assertThat(users.findById(otherId).orElseThrow().name()).isEqualTo("Other");
        // The original token continues to identify this user even though its display-name claim is older.
        mvc.perform(get("/identity/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Maria Silva"));
    }

    @Test
    void omissionPreservesValuesNullClearsOptionalsAndEmptyObjectPreservesTimestamp() throws Exception {
        patchProfile("{}").andExpect(status().isOk());
        assertThat(users.findById(before.id()).orElseThrow()).isEqualTo(before);
        patchProfile("{\"name\":\"New name\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.rg").value(before.rg()))
                .andExpect(jsonPath("$.phoneNumber").value(before.phoneNumber()))
                .andExpect(jsonPath("$.birthDate").value("2000-05-17"));
        patchProfile("{\"birthDate\":null,\"rg\":null,\"phoneNumber\":null,\"profilePhotoStorageKey\":null}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("New name"))
                .andExpect(jsonPath("$.birthDate").isEmpty()).andExpect(jsonPath("$.rg").isEmpty())
                .andExpect(jsonPath("$.phoneNumber").isEmpty());
        entityManager.clear();
        UserDetails saved = users.findById(before.id()).orElseThrow();
        assertThat(saved.rg()).isNull();
        assertThat(saved.birthDate()).isNull();
        assertThat(saved.phoneNumber()).isNull();
    }

    @Test
    void preservesExistingCanonicalizationForBlankOptionalsAndSameRg() throws Exception {
        patchProfile("{\"rg\":\"12.345.678-9\",\"phoneNumber\":\" \"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.rg").value(before.rg()))
                .andExpect(jsonPath("$.phoneNumber").isEmpty());
        patchProfile("{\"rg\":\" \"}").andExpect(status().isOk()).andExpect(jsonPath("$.rg").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"cpf", "id", "userId", "status", "roles", "permissions", "createdAt", "updatedAt",
            "disabledAt", "email", "password", "profilePhotoUrl", "unknown"})
    void rejectsForbiddenOrUnknownFieldsWithoutAnyPartialUpdate(String field) throws Exception {
        patchProfile("{\"name\":\"Must not change\",\"" + field + "\":null}")
                .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        entityManager.clear();
        assertThat(users.findById(before.id()).orElseThrow()).isEqualTo(before);
    }

    @ParameterizedTest
    @MethodSource("invalidBodies")
    void rejectsInvalidValuesAndTypesWithoutChangingProfile(String body) throws Exception {
        patchProfile(body).andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.timestamp").exists());
        entityManager.clear();
        assertThat(users.findById(before.id()).orElseThrow()).isEqualTo(before);
    }

    static Stream<String> invalidBodies() {
        return Stream.of("[]", "null", "{\"name\":null}", "{\"name\":\" \"}", "{\"name\":17}",
                "{\"name\":\"" + "a".repeat(151) + "\"}",
                "{\"birthDate\":\"2025-02-30\"}", "{\"birthDate\":\"17/05/2000\"}",
                "{\"birthDate\":2000}", "{\"rg\":\"letters\"}", "{\"phoneNumber\":false}",
                "{\"name\":\"Valid\",\"phoneNumber\":\"letters\"}",
                "{\"rg\":\"" + "1".repeat(21) + "\"}",
                "{\"phoneNumber\":\"" + "1".repeat(21) + "\"}",
                "{\"profilePhotoStorageKey\":\"https://example.com/photo.png\"}",
                "{\"profilePhotoStorageKey\":\" \"}");
    }

    @Test
    void conflictingRgReturnsConflictWithoutChangingOtherFields() throws Exception {
        patchProfile("{\"name\":\"Must not change\",\"rg\":\"98.765.432-1\"}")
                .andExpect(status().isConflict()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("USER_RG_ALREADY_IN_USE"));
        entityManager.clear();
        assertThat(users.findById(before.id()).orElseThrow()).isEqualTo(before);
    }

    @Test
    void globalMediaUploadCanBeAssociatedPreservedAndUnlinkedWithoutDeletingBytes() throws Exception {
        byte[] png = {(byte)137, 80, 78, 71, 13, 10, 26, 10};
        String response = mvc.perform(multipart("/media/images")
                        .file(new MockMultipartFile("file", "photo.png", "image/png", png))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String key = json.readTree(response).get("storageKey").asString();
        patchProfile("{\"profilePhotoStorageKey\":\"" + key + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.profilePhotoStorageKey").value(key));
        patchProfile("{\"name\":\"Photo retained\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.profilePhotoStorageKey").value(key));
        patchProfile("{\"profilePhotoStorageKey\":null}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.profilePhotoStorageKey").isEmpty());
        mvc.perform(get("/media/images").param("key", key).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(content().bytes(png));
    }

    @Test
    void missingMediaReferenceReturnsNotFoundAndPreservesProfile() throws Exception {
        patchProfile("{\"name\":\"Unchanged\",\"profilePhotoStorageKey\":\"images/2026/09/24/550e8400-e29b-41d4-a716-446655440000.png\"}")
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("IMAGE_NOT_FOUND"));
        assertThat(users.findById(before.id()).orElseThrow()).isEqualTo(before);
    }

    @Test
    void unauthenticatedReadAndUpdateAreRejected() throws Exception {
        mvc.perform(get("/identity/me")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/identity/me").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"No\"}"))
                .andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(users.findById(before.id()).orElseThrow()).isEqualTo(before);
    }

    private org.springframework.test.web.servlet.ResultActions patchProfile(String body) throws Exception {
        return mvc.perform(patch("/identity/me").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void complementaryProfileIsOptionalAndUpsertIsOwnedByPrincipal() throws Exception {
        mvc.perform(get("/identity/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.profileCompletionPending").value(true));
        mvc.perform(get("/identity/me/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.profileCompletionPending").value(true))
                .andExpect(jsonPath("$.workProfile").isEmpty()).andExpect(jsonPath("$.address").isEmpty());
        assertThat(profileCount(before.id())).isZero();

        putComplementary("{\"workProfile\":{\"occupation\":\" Mechanic \"}}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.workProfile.occupation").value("Mechanic"))
                .andExpect(jsonPath("$.profileCompletionPending").value(true));
        entityManager.clear();
        assertThat(profileCount(before.id())).isEqualTo(1);

        String complete = """
                {"workProfile":{"occupation":" Mechanic ","workplace":" Garage "},
                 "address":{"postalCode":"12345-678","street":" Main Street ","number":"S/N",
                            "neighborhood":" Center ","city":" City ","state":"sp"}}
                """;
        mvc.perform(put("/identity/me/profile").queryParam("userId", otherId.toString())
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(complete))
                .andExpect(status().isOk()).andExpect(jsonPath("$.profileCompletionPending").value(false))
                .andExpect(jsonPath("$.address.postalCode").value("12345678"))
                .andExpect(jsonPath("$.address.state").value("SP"));
        entityManager.clear();
        mvc.perform(get("/identity/me/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.profileCompletionPending").value(false))
                .andExpect(jsonPath("$.workProfile.workplace").value("Garage"));
        mvc.perform(get("/identity/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.profileCompletionPending").value(false))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        patchProfile("{\"name\":\"New Name\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.profileCompletionPending").value(false));
        putComplementary(complete.replace("Garage", "Workshop"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.workProfile.workplace").value("Workshop"));
        entityManager.clear();
        assertThat(profileCount(before.id())).isEqualTo(1);
        assertThat(profileCount(otherId)).isZero();
        assertThat(users.findById(otherId).orElseThrow().name()).isEqualTo("Other");
        assertThat(users.findById(before.id()).orElseThrow().cpf()).isEqualTo(before.cpf());

        putComplementary("{}").andExpect(status().isOk()).andExpect(jsonPath("$.profileCompletionPending").value(true));
        entityManager.clear();
        mvc.perform(get("/identity/me/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.workProfile").isEmpty())
                .andExpect(jsonPath("$.address").isEmpty()).andExpect(jsonPath("$.profileCompletionPending").value(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]", "{\"userId\":1}", "{\"status\":\"DISABLED\"}",
            "{\"workProfile\":{\"userId\":1}}", "{\"workProfile\":{\"occupation\":42}}",
            "{\"address\":{\"unknown\":null}}", "{\"address\":{\"postalCode\":\"123\"}}",
            "{\"address\":{\"state\":\"ZZ\"}}", "{\"address\":false}", "{\"workProfile\":[]}",
            "{\"id\":1}", "{\"address\":{\"userId\":1}}", "{\"address\":{\"id\":1}}",
            "{\"address\":{\"postalCode\":12345678}}", "{\"address\":{\"number\":12}}",
            "{\"workProfile\":{\"workplace\":true}}"})
    void complementaryProfileRejectsInvalidDataAndArbitraryOwnership(String body) throws Exception {
        putComplementary("{\"workProfile\":{\"occupation\":\"Original\"}}")
                .andExpect(status().isOk());
        putComplementary(body).andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        entityManager.clear();
        mvc.perform(get("/identity/me/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.workProfile.occupation").value("Original"));
        assertThat(profileCount(otherId)).isZero();
    }

    @Test
    void complementaryProfileRequiresAuthentication() throws Exception {
        mvc.perform(get("/identity/me/profile")).andExpect(status().isUnauthorized());
        mvc.perform(put("/identity/me/profile").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        assertThat(profileCount(before.id())).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"workProfile\":null,\"address\":null}", "{}",
            "{\"workProfile\":{},\"address\":{}}",
            "{\"workProfile\":{\"occupation\":\"  \",\"workplace\":\"  \"},"
                    + "\"address\":{\"postalCode\":\" \",\"street\":\" \",\"number\":\" \","
                    + "\"complement\":\" \",\"neighborhood\":\" \",\"city\":\" \",\"state\":\" \"}}"})
    void replacementClearsPopulatedBlocksAndDerivedCompletion(String body) throws Exception {
        putComplementary("""
                {"workProfile":{"occupation":"Mechanic","workplace":"Garage"},
                 "address":{"postalCode":"12345678","street":"Street","number":"1",
                            "complement":"Apartment","neighborhood":"Center","city":"City","state":"SP"}}
                """).andExpect(status().isOk()).andExpect(jsonPath("$.profileCompletionPending").value(false));
        putComplementary(body).andExpect(status().isOk())
                .andExpect(jsonPath("$.workProfile").isEmpty()).andExpect(jsonPath("$.address").isEmpty())
                .andExpect(jsonPath("$.profileCompletionPending").value(true));
        entityManager.clear();
        mvc.perform(get("/identity/me/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.workProfile").isEmpty())
                .andExpect(jsonPath("$.address").isEmpty());
        mvc.perform(get("/identity/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.profileCompletionPending").value(true));
        assertThat(profileCount(before.id())).isEqualTo(1);
        assertThat(profileCount(otherId)).isZero();
        assertThat(users.findById(before.id()).orElseThrow()).isEqualTo(before);
    }

    @Test
    void replacementRemovesOmittedBlocksAndFieldsRatherThanMergingThem() throws Exception {
        putComplementary("""
                {"workProfile":{"occupation":"Mechanic","workplace":"Garage"},"address":{"city":"City"}}
                """).andExpect(status().isOk());
        putComplementary("{\"workProfile\":{\"occupation\":\"Driver\"}}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.workProfile.workplace").isEmpty())
                .andExpect(jsonPath("$.address").isEmpty());
        putComplementary("{\"address\":{\"city\":\"Another city\"}}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.workProfile").isEmpty())
                .andExpect(jsonPath("$.address.city").value("Another city"));
        entityManager.clear();
        mvc.perform(get("/identity/me/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.workProfile").isEmpty())
                .andExpect(jsonPath("$.address.city").value("Another city"));
    }

    @ParameterizedTest
    @CsvSource({"workProfile,occupation,150", "workProfile,workplace,150", "address,street,150",
            "address,number,20", "address,complement,150", "address,neighborhood,100", "address,city,100"})
    void textLimitsApplyAfterTrimmingAndInvalidReplacementDoesNotMutate(String block, String field, int limit) throws Exception {
        String accepted = "a".repeat(limit);
        putComplementary(json.writeValueAsString(Map.of(block, Map.of(field, " " + accepted + " "))))
                .andExpect(status().isOk()).andExpect(jsonPath("$." + block + "." + field).value(accepted));
        putComplementary(json.writeValueAsString(Map.of(block, Map.of(field, accepted + "a"))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        entityManager.clear();
        mvc.perform(get("/identity/me/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$." + block + "." + field).value(accepted));
        assertThat(profileCount(otherId)).isZero();
    }

    private long profileCount(Long id) {
        return entityManager.createQuery("select count(p) from UserProfileEntity p where p.userId = :id", Long.class)
                .setParameter("id", id).getSingleResult();
    }

    private org.springframework.test.web.servlet.ResultActions putComplementary(String body) throws Exception {
        return mvc.perform(put("/identity/me/profile").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
