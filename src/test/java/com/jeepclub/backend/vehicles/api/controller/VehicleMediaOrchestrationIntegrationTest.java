package com.jeepclub.backend.vehicles.api.controller;

import com.jeepclub.backend.iam.authentication.core.application.service.security.AccessTokenAuthenticationService;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.platform.security.jwt.JwtAuthenticatedUser;
import com.jeepclub.backend.platform.security.jwt.JwtTokenParser;
import com.jeepclub.backend.platform.storage.image.ImageUpload;
import com.jeepclub.backend.shared.storage.*;
import com.jeepclub.backend.shared.storage.exception.StorageOperationException;
import com.jeepclub.backend.vehicles.core.application.service.vehicle.VehicleService;
import com.jeepclub.backend.vehicles.core.domain.enums.FuelType;
import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import com.jeepclub.backend.vehicles.core.port.UserPort;
import com.jeepclub.backend.vehicles.core.repository.VehicleRepository;
import com.jeepclub.backend.vehicles.infra.persistence.jpa.VehicleJpaRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class VehicleMediaOrchestrationIntegrationTest {
    static final byte[] PNG = {(byte)137,80,78,71,13,10,26,10,1};
    static final String OLD = "images/2026/10/01/550e8400-e29b-41d4-a716-446655440000.png";
    static final String DATA = """
        {"nickname":"Media","plate":"MED1D23","renavam":"38249206428","brand":"Jeep","model":"Wrangler",
        "manufacturingYear":2023,"modelYear":2024,"color":"Green","seatingCapacity":5,
        "fuelType":"DIESEL","engineDisplacement":2.0,"towing":true}
        """;
    @Autowired MockMvc mvc;
    @Autowired VehicleService vehicles;
    @Autowired VehicleJpaRepository jpa;
    @Autowired PlatformTransactionManager transactions;
    @Autowired ObjectMapper json;
    @Autowired Clock clock;
    @MockitoSpyBean VehicleRepository repository;
    @MockitoBean FileStorage storage;
    @MockitoBean JwtTokenParser parser;
    @MockitoBean UserAuthoritiesProvider authorities;
    @MockitoBean AccessTokenAuthenticationService authentication;
    @MockitoBean UserPort owners;
    @BeforeEach void setup() {
        jpa.deleteAll();
        when(owners.existsById(10L)).thenReturn(true);
        when(owners.existsActiveById(10L)).thenReturn(true);
        when(storage.store(any(), eq("images"))).thenAnswer(a -> new StoredFile(key()));
        auth("owner", 10L, List.of());
    }
    static String key() { return "images/2026/10/18/" + UUID.randomUUID() + ".png"; }
    void auth(String token, Long id, List<String> permissions) {
        when(parser.parseAndValidate(token)).thenReturn(new JwtAuthenticatedUser(id, 100L + id, "Synthetic media actor", Instant.now(clock).plusSeconds(3600)));
        when(authorities.findAuthorityCodesByUserId(id)).thenReturn(permissions);
    }
    MockMultipartFile data(String value) {
        return new MockMultipartFile("vehicle", "vehicle.json", "application/json", value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    MockMultipartFile photo(byte[] bytes) { return new MockMultipartFile("photo", "photo.png", "image/png", bytes); }
    Vehicle create(ImageUpload upload, String brand) {
        return vehicles.create("Media", upload, "MED1D23", "38249206428", brand, "Wrangler", 2023, 2024,
            "Green", 5, FuelType.DIESEL, 2.0, true, 10L);
    }
    ImageUpload upload() { return new ImageUpload("photo.png", "image/png", PNG); }

    @ParameterizedTest
    @CsvSource({"false,legacy", "true,legacy", "false,creationGallery", "true,creationGallery", "false,updateGallery", "true,updateGallery"})
    void requestReadFailureNeverCreatesOrUpdatesVehicleOrUploadsFiles(boolean admin, String route) throws Exception {
        auth("read-actor", 10L, List.of("VEHICLES_VEHICLE_CREATE", "VEHICLES_VEHICLE_UPDATE"));
        boolean editing = route.equals("updateGallery");
        Long id = editing ? create(null, "Jeep").getId() : null;
        long before = jpa.count();
        clearInvocations(repository, storage);
        String path = editing ? "/vehicles/gallery/" + (admin ? "admin/" : "member/") + id
                : "/vehicles/include/" + (admin ? "admin/10" : "member");
        var request = multipart(editing ? org.springframework.http.HttpMethod.PUT : org.springframework.http.HttpMethod.POST, path);
        if (editing) request.file(new MockMultipartFile("gallery", "", "application/json",
                "{\"expectedRevision\":0,\"images\":[{\"uploadIndex\":0,\"position\":0,\"primary\":true},{\"uploadIndex\":1,\"position\":1,\"primary\":false}]}".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        else request.file(data(DATA));
        String imagePart = route.equals("legacy") ? "photo" : "photos";
        if (!route.equals("legacy")) request.file(new MockMultipartFile("photos", "valid.png", "image/png", PNG));
        var broken = spy(new MockMultipartFile(imagePart, "sensitive-filename.png", "image/png", PNG));
        doThrow(new java.io.IOException("sensitive-path-storageKey-bytes")).when(broken).getBytes();
        mvc.perform(request.file(broken).header("Authorization", "Bearer read-actor"))
                .andExpect(status().isInternalServerError()).andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("IMAGE_UPLOAD_READ_FAILED")).andExpect(jsonPath("$.status").value(500))
                .andExpect(r -> assertThat(r.getResponse().getContentAsString()).doesNotContain("sensitive-", "IOException", "stackTrace"));
        assertThat(jpa.count()).isEqualTo(before);
        if (editing) assertThat(jpa.findById(id).orElseThrow()).satisfies(v -> {
            assertThat(v.getGalleryRevision()).isZero(); assertThat(v.getPhoto()).isNull(); assertThat(v.getUpdatedAt()).isNull();
        });
        verify(repository, never()).save(any()); verifyNoInteractions(storage);
    }

    @Test void multipartUploadCommitsImageReferenceAndPrincipalOwnership() throws Exception {
        mvc.perform(multipart("/vehicles/include/member").file(data(DATA)).file(photo(PNG)).header("Authorization", "Bearer owner"))
            .andExpect(status().isCreated());
        assertThat(jpa.findAll()).singleElement().satisfies(v -> {
            assertThat(v.getOwnerId()).isEqualTo(10L); assertThat(v.getPhoto()).startsWith("images/");
        });
        verify(storage).store(any(), eq("images")); verify(storage, never()).delete(anyString());
    }
    @Test void jsonWithoutPhotoDoesNotWriteStorage() throws Exception {
        mvc.perform(post("/vehicles/include/member").contentType("application/json").content(DATA).header("Authorization", "Bearer owner"))
            .andExpect(status().isCreated());
        assertThat(jpa.findAll()).singleElement().satisfies(v -> assertThat(v.getPhoto()).isNull());
        verify(storage, never()).store(any(), anyString());
    }
    @Test void multipartWithoutPhotoIsValid() throws Exception {
        mvc.perform(multipart("/vehicles/include/member").file(data(DATA)).header("Authorization", "Bearer owner"))
            .andExpect(status().isCreated());
        assertThat(jpa.findAll()).singleElement().satisfies(v -> assertThat(v.getPhoto()).isNull());
        verify(storage, never()).store(any(), anyString());
    }
    @Test void invalidDtoAndInvalidSignatureNeverStoreAnImage() throws Exception {
        mvc.perform(multipart("/vehicles/include/member").file(data(DATA.replace("\"Jeep\"", "\"\""))).file(photo(PNG))
            .header("Authorization", "Bearer owner")).andExpect(status().isBadRequest());
        mvc.perform(multipart("/vehicles/include/member").file(data(DATA)).file(photo(new byte[]{1,2,3}))
            .header("Authorization", "Bearer owner")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_IMAGE"));
        assertThat(jpa.count()).isZero(); verify(storage, never()).store(any(), anyString());
    }
    @Test void emptyOversizedAndMissingStructuredPartAreRejected() throws Exception {
        for (byte[] bytes : List.of(new byte[0], new byte[ImageUpload.MAX_BYTES + 1])) {
            mvc.perform(multipart("/vehicles/include/member").file(data(DATA)).file(photo(bytes)).header("Authorization", "Bearer owner"))
                .andExpect(status().isBadRequest());
        }
        mvc.perform(multipart("/vehicles/include/member").file(photo(PNG)).header("Authorization", "Bearer owner"))
            .andExpect(status().isBadRequest());
        assertThat(jpa.count()).isZero(); verify(storage, never()).store(any(), anyString());
    }
    @Test void creationCannotAssociateArbitraryExistingStorageKey() throws Exception {
        for (String field : List.of("photo", "storageKey", "ownerId")) {
            String body = DATA.trim().replaceFirst("\\}$", ",\"" + field + "\":\"untrusted\"}");
            mvc.perform(post("/vehicles/include/member").contentType("application/json").content(body).header("Authorization", "Bearer owner"))
                .andExpect(status().isBadRequest());
        }
        assertThat(jpa.count()).isZero(); verify(storage, never()).store(any(), anyString());
    }
    @Test void structuredPartMustBeAnObject() throws Exception {
        for (String body : List.of("null", "[]", "1", "\"vehicle\"")) {
            mvc.perform(multipart("/vehicles/include/member").file(data(body)).file(photo(PNG)).header("Authorization", "Bearer owner"))
                .andExpect(status().isBadRequest());
        }
        assertThat(jpa.count()).isZero(); verify(storage, never()).store(any(), anyString());
    }
    @Test void duplicateUnknownAndTextualImagePartsAreRejectedBeforeStorage() throws Exception {
        mvc.perform(multipart("/vehicles/include/member").file(data(DATA)).file(photo(PNG)).file(photo(PNG))
            .header("Authorization", "Bearer owner")).andExpect(status().isBadRequest());
        mvc.perform(multipart("/vehicles/include/member").file(data(DATA))
            .file(new MockMultipartFile("unknown", "photo.png", "image/png", PNG)).header("Authorization", "Bearer owner"))
            .andExpect(status().isBadRequest());
        mvc.perform(multipart("/vehicles/include/member").file(data(DATA)).param("photo", OLD)
            .header("Authorization", "Bearer owner")).andExpect(status().isBadRequest());
        assertThat(jpa.count()).isZero(); verify(storage, never()).store(any(), anyString());
    }
    @Test void unavailableStorageIsSanitized503WithoutJpaRow() throws Exception {
        when(storage.store(any(), anyString())).thenThrow(new StorageOperationException(StorageOperationException.Operation.WRITE,
            new java.io.IOException("private-path-or-secret")));
        mvc.perform(multipart("/vehicles/include/member").file(data(DATA)).file(photo(PNG)).header("Authorization", "Bearer owner"))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("IMAGE_STORAGE_UNAVAILABLE"))
            .andExpect(r -> assertThat(r.getResponse().getContentAsString()).doesNotContain("private-path-or-secret"));
        assertThat(jpa.count()).isZero(); verify(storage, never()).delete(anyString());
    }
    @Test void adminMultipartRequiresSpecificPermissionAndActiveOwner() throws Exception {
        mvc.perform(multipart("/vehicles/include/admin/77").file(data(DATA)).file(photo(PNG))).andExpect(status().isUnauthorized());
        auth("admin", 99L, List.of("VEHICLES_VEHICLE_READ"));
        mvc.perform(multipart("/vehicles/include/admin/77").file(data(DATA)).file(photo(PNG)).header("Authorization", "Bearer admin"))
            .andExpect(status().isForbidden());
        verify(storage, never()).store(any(), anyString());
        auth("admin", 99L, List.of("VEHICLES_VEHICLE_CREATE")); when(owners.existsById(77L)).thenReturn(true);
        mvc.perform(multipart("/vehicles/include/admin/77").file(data(DATA)).file(photo(PNG)).header("Authorization", "Bearer admin"))
            .andExpect(status().isConflict());
        verify(storage, never()).store(any(), anyString());
        when(owners.existsActiveById(77L)).thenReturn(true);
        mvc.perform(multipart("/vehicles/include/admin/77").file(data(DATA)).file(photo(PNG)).header("Authorization", "Bearer admin"))
            .andExpect(status().isCreated());
        assertThat(jpa.findAll()).singleElement().satisfies(v -> assertThat(v.getOwnerId()).isEqualTo(77L));
    }
    @Test void actualJpaConstraintFailureCompensatesFreshImageAndRetainsPreviousReference() throws Exception {
        repository.save(Vehicle.create("Existing", OLD, "MED1D23", "38249206428", "Jeep", "Wrangler", 2023, 2024,
            "Green", 5, FuelType.DIESEL, 2.0, true, 20L, Instant.now(clock)));
        doReturn(false).when(repository).existsByPlate("MED1D23");
        doReturn(false).when(repository).existsByRenavam("38249206428");
        mvc.perform(multipart("/vehicles/include/member").file(data(DATA)).file(photo(PNG)).header("Authorization", "Bearer owner"))
            .andExpect(status().isConflict());
        var deleted = ArgumentCaptor.forClass(String.class); verify(storage).delete(deleted.capture());
        assertThat(deleted.getValue()).isNotEqualTo(OLD);
        assertThat(jpa.findAll()).singleElement().satisfies(v -> { assertThat(v.getPhoto()).isEqualTo(OLD); assertThat(v.getOwnerId()).isEqualTo(20L); });
    }
    @Test void domainValidationAfterUploadAlsoCompensates() {
        assertThatThrownBy(() -> create(upload(), " ")).isInstanceOf(IllegalArgumentException.class);
        assertThat(jpa.count()).isZero(); verify(storage).delete(anyString());
    }
    @Test void outerRollbackAfterSuccessfulJpaSaveCompensatesImage() {
        Long id = new TransactionTemplate(transactions).execute(tx -> {
            var vehicle = create(upload(), "Jeep"); tx.setRollbackOnly(); return vehicle.getId();
        });
        assertThat(jpa.findById(id)).isEmpty(); verify(storage).delete(anyString());
    }
    @Test void cleanupFailurePreservesTheOriginalErrorAndRollsBackJpa() {
        var original = new IllegalStateException("Synthetic failure after persistence");
        doThrow(new IllegalStateException("private-cleanup-path")).when(storage).delete(anyString());
        assertThatThrownBy(() -> new TransactionTemplate(transactions).execute(tx -> {
            create(upload(), "Jeep"); throw original;
        })).isSameAs(original);
        assertThat(jpa.count()).isZero();
    }
    @Test void concurrentDuplicateCreationRetainsWinnerImageAndCompensatesOnlyLoser() throws Exception {
        var barrier = new CyclicBarrier(2);
        when(storage.store(any(), eq("images"))).thenAnswer(a -> { var fresh = new StoredFile(key()); barrier.await(20, TimeUnit.SECONDS); return fresh; });
        var executor = Executors.newFixedThreadPool(2);
        Supplier<Object> work = () -> { try { return create(upload(), "Jeep"); } catch (RuntimeException ex) { return ex; } };
        try {
            var a = executor.submit(work::get); var b = executor.submit(work::get);
            var outcomes = List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
            assertThat(outcomes.stream().filter(Vehicle.class::isInstance)).hasSize(1);
            assertThat(outcomes.stream().filter(RuntimeException.class::isInstance)).hasSize(1);
            String winner = ((Vehicle) outcomes.stream().filter(Vehicle.class::isInstance).findFirst().orElseThrow()).getPhoto();
            var deleted = ArgumentCaptor.forClass(String.class); verify(storage).delete(deleted.capture());
            assertThat(deleted.getValue()).isNotEqualTo(winner);
            assertThat(jpa.findAll()).singleElement().satisfies(v -> assertThat(v.getPhoto()).isEqualTo(winner));
        } finally { executor.shutdownNow(); }
    }
    @Test void openApiHasMultipartStructuredDataBinaryPhotoAndNoCreationStorageKey() throws Exception {
        var result = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        var document = json.readTree(result.getResponse().getContentAsString());
        for (String path : List.of("/vehicles/include/member", "/vehicles/include/admin/{memberId}")) {
            var operation = document.path("paths").path(path).path("post");
            assertThat(operation.path("requestBody").path("content").has("multipart/form-data")).isTrue();
            assertThat(operation.path("requestBody").path("content").has("application/json")).isTrue();
            assertThat(operation.path("responses").has("503")).isTrue();
            assertThat(operation.path("responses").path("500").path("description").asText()).contains("IMAGE_UPLOAD_READ_FAILED");
            var schema = operation.path("requestBody").path("content").path("multipart/form-data").path("schema");
            if (schema.has("$ref")) {
                String ref = schema.path("$ref").asText(); schema = document.path("components").path("schemas").path(ref.substring(ref.lastIndexOf('/') + 1));
            }
            assertThat(schema.path("properties").has("vehicle")).isTrue();
            assertThat(schema.path("properties").path("photo").path("format").asText()).isEqualTo("binary");
            assertThat(schema.path("required").toString()).contains("vehicle").doesNotContain("photo");
            assertThat(operation.path("responses").path("503").path("content").has("application/problem+json")).isTrue();
        }
        var fields = document.path("components").path("schemas").path("IncludeRequestDTO").path("properties");
        assertThat(fields.has("photo")).isFalse(); assertThat(fields.has("storageKey")).isFalse();
        for (var route : Map.of("/media/images", "post", "/dependents", "post", "/dependents/{id}", "put",
                "/dependents/{id}/photo", "patch", "/users/{userId}/dependents/{id}/photo", "patch",
                "/vehicles/gallery/member/{vehicleId}", "put", "/vehicles/gallery/admin/{vehicleId}", "put").entrySet()) {
            var error = document.path("paths").path(route.getKey()).path(route.getValue()).path("responses").path("500");
            assertThat(error.path("description").asText()).as(route.getKey()).contains("IMAGE_UPLOAD_READ_FAILED");
            assertThat(error.path("content").has("application/problem+json")).isTrue();
        }
    }
}
