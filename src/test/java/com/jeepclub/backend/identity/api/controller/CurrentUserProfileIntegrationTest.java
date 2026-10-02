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
}
