package com.jeepclub.backend.vehicles.api.controller;

import com.jeepclub.backend.iam.authentication.core.application.service.security.AccessTokenAuthenticationService;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.platform.security.jwt.JwtAuthenticatedUser;
import com.jeepclub.backend.platform.security.jwt.JwtTokenParser;
import com.jeepclub.backend.platform.storage.image.ImageUpload;
import com.jeepclub.backend.shared.storage.FileStorage;
import com.jeepclub.backend.shared.storage.StoredFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Real servlet multipart limits are not exercised by MockMvc. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "server.address=127.0.0.1")
@ActiveProfiles("test")
class VehicleMediaMultipartHttpIntegrationTest {
    @LocalServerPort int port;
    @Autowired Clock clock;
    @Autowired com.jeepclub.backend.vehicles.infra.persistence.jpa.VehicleJpaRepository vehicles;
    @MockitoBean FileStorage storage;
    @MockitoBean com.jeepclub.backend.vehicles.core.port.UserPort owners;
    @MockitoBean JwtTokenParser parser;
    @MockitoBean UserAuthoritiesProvider authorities;
    @MockitoBean AccessTokenAuthenticationService authentication;

    @ParameterizedTest
    @CsvSource({"false,false", "true,false", "false,true", "true,true"})
    void realServletRejectsAmbiguousMultipartBeforeAnySideEffects(boolean admin, boolean editing) throws Exception {
        multipartActor();
        Long id = editing ? existingVehicle() : null;
        String path = editing ? "/vehicles/gallery/" + (admin ? "admin/" : "member/") + id
                : "/vehicles/include/" + (admin ? "admin/10" : "member");
        var structured = field(editing ? "gallery" : "vehicle", "application/json",
                editing ? "{\"expectedRevision\":0,\"images\":[]}" : uniqueData());
        var invalid = new ArrayList<List<HttpPart>>();
        invalid.add(List.of(field("unexpected", "text/plain", "value"), image("photos")));
        invalid.add(List.of(field("photo", "text/plain", "images/unauthorized.png")));
        invalid.add(List.of(field("photos", "text/plain", "images/unauthorized.png")));
        invalid.add(List.of(image("unknown")));
        invalid.add(List.of(structured));
        invalid.add(List.of(new HttpPart(structured.name(), "data.json", "application/json", structured.bytes())));
        invalid.add(List.of(image("photo"), image("photo")));
        invalid.add(List.of(image("photo"), image("photos")));
        invalid.add(Collections.nCopies(6, image("photos")));
        if (!editing) {
            invalid.add(List.of(field("gallery", "application/json", "{\"images\":[]}"), image("photo")));
            invalid.add(List.of(field("gallery", "application/json", "{\"images\":[]}"), field("gallery", "application/json", "{\"images\":[]}")));
        } else {
            invalid.add(List.of(field("vehicle", "application/json", uniqueData())));
            invalid.add(List.of(image("photo")));
        }
        long before = vehicles.count();
        for (var extra : invalid) {
            var parts = new ArrayList<HttpPart>(); parts.add(structured); parts.addAll(extra);
            var response = sendMultipart(path, editing ? "PUT" : "POST", parts);
            assertThat(response.statusCode()).as("%s parts %s: %s", path, extra.stream().map(HttpPart::name).toList(), response.body()).isEqualTo(400);
            assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/problem+json");
            assertThat(response.body()).contains("\"code\":\"INVALID_IMAGE\"", "\"status\":400");
            assertThat(vehicles.count()).isEqualTo(before);
            if (editing) assertThat(vehicles.findById(id).orElseThrow()).satisfies(v -> {
                assertThat(v.getGalleryRevision()).isZero(); assertThat(v.getPhoto()).isNull();
            });
            verifyNoInteractions(storage);
        }
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    void realServletAcceptsTypedJsonFieldsOptionalLegacyAndMultiplePhotos(boolean admin) throws Exception {
        multipartActor();
        String create = "/vehicles/include/" + (admin ? "admin/10" : "member");
        for (int count : new int[]{0, 1, 2, 3, 4, 5}) {
            var parts = new ArrayList<HttpPart>();
            String data = uniqueData(); parts.add(field("vehicle", "application/json", data));
            for (int i = 0; i < count; i++) parts.add(image("photos"));
            if (count == 1) parts.add(field("gallery", "application/json", "{\"images\":[{\"uploadIndex\":0,\"position\":0,\"primary\":true}]}"));
            var response = sendMultipart(create, "POST", parts);
            assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
        }
        assertThat(sendMultipart(create, "POST", List.of(field("vehicle", "application/json", uniqueData()), image("photo"))).statusCode()).isEqualTo(201);
        Long id = existingVehicle();
        String gallery = "/vehicles/gallery/" + (admin ? "admin/" : "member/") + id;
        var response = sendMultipart(gallery, "PUT", List.of(field("gallery", "application/json",
                "{\"expectedRevision\":0,\"images\":[{\"uploadIndex\":0,\"position\":0,\"primary\":true}]}"), image("photos")));
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(vehicles.findById(id).orElseThrow().getGalleryRevision()).isEqualTo(1);
    }

    void multipartActor() {
        when(owners.existsById(10L)).thenReturn(true); when(owners.existsActiveById(10L)).thenReturn(true);
        when(parser.parseAndValidate("multipart-actor")).thenReturn(new JwtAuthenticatedUser(10L, 110L, "Synthetic multipart actor", Instant.now(clock).plusSeconds(3600)));
        when(authorities.findAuthorityCodesByUserId(10L)).thenReturn(List.of("VEHICLES_VEHICLE_CREATE", "VEHICLES_VEHICLE_UPDATE"));
        when(storage.store(any(), eq("images"))).thenAnswer(a -> new StoredFile("images/2026/10/29/" + UUID.randomUUID() + ".png"));
    }

    String uniqueData() {
        return VehicleMediaOrchestrationIntegrationTest.DATA.replace("MED1D23", VehicleGalleryIntegrationTest.plate())
                .replace("38249206428", VehicleGalleryIntegrationTest.renavam());
    }

    Long existingVehicle() throws Exception {
        String data = uniqueData();
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/vehicles/include/member"))
                .header("Authorization", "Bearer multipart-actor").header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(data)).build();
        var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
        return vehicles.findAll().stream().filter(v -> data.contains(v.getPlate())).findFirst().orElseThrow().getId();
    }

    record HttpPart(String name, String filename, String type, byte[] bytes) {}
    HttpPart field(String name, String type, String value) { return new HttpPart(name, null, type, value.getBytes(StandardCharsets.UTF_8)); }
    HttpPart image(String name) { return new HttpPart(name, "photo.png", "image/png", VehicleMediaOrchestrationIntegrationTest.PNG); }

    @Test void realServletPreservesTransport413AndStructuredMediaType415() throws Exception {
        multipartActor(); long before = vehicles.count();
        var oversized = sendMultipart("/vehicles/include/member", "POST", List.of(field("vehicle", "application/json", uniqueData()),
                new HttpPart("photo", "photo.png", "image/png", new byte[10 * 1024 * 1024 + 1])));
        assertThat(oversized.statusCode()).as(oversized.body()).isEqualTo(413);
        assertThat(oversized.body()).contains("\"status\":413");
        for (boolean admin : new boolean[]{false, true}) {
            var wrongType = sendMultipart("/vehicles/include/" + (admin ? "admin/10" : "member"), "POST",
                    List.of(field("vehicle", "text/plain", uniqueData()), image("photo")));
            assertThat(wrongType.statusCode()).as(wrongType.body()).isEqualTo(415);
        }
        assertThat(vehicles.count()).isEqualTo(before); verifyNoInteractions(storage);
    }

    HttpResponse<String> sendMultipart(String path, String method, List<HttpPart> parts) throws Exception {
        String boundary = "Regression" + UUID.randomUUID(); var body = new ByteArrayOutputStream();
        for (var part : parts) {
            body.writeBytes(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + part.name() + "\""
                    + (part.filename() == null ? "" : "; filename=\"" + part.filename() + "\"")
                    + "\r\nContent-Type: " + part.type() + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            body.writeBytes(part.bytes()); body.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        body.writeBytes(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer multipart-actor").header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .method(method, HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test void realServletAcceptsFiveMbImageAndRejectsOneExtraByte() throws Exception {
        when(owners.existsById(10L)).thenReturn(true);
        when(owners.existsActiveById(10L)).thenReturn(true);
        when(parser.parseAndValidate("owner")).thenReturn(new JwtAuthenticatedUser(10L, 110L,
            "Synthetic HTTP actor", Instant.now(clock).plusSeconds(3600)));
        when(authorities.findAuthorityCodesByUserId(10L)).thenReturn(List.of());
        when(storage.store(any(), eq("images"))).thenReturn(new StoredFile("images/2026/10/18/" + UUID.randomUUID() + ".png"));
        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        for (int size : new int[]{ImageUpload.MAX_BYTES, ImageUpload.MAX_BYTES + 1}) {
            byte[] bytes = new byte[size]; System.arraycopy(VehicleMediaOrchestrationIntegrationTest.PNG, 0, bytes, 0, VehicleMediaOrchestrationIntegrationTest.PNG.length);
            String boundary = "MediaBoundary" + UUID.randomUUID().toString().replace("-", "");
            var body = new ByteArrayOutputStream();
            body.writeBytes(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"vehicle\"\r\nContent-Type: application/json\r\n\r\n"
                + VehicleMediaOrchestrationIntegrationTest.DATA + "\r\n--" + boundary
                + "\r\nContent-Disposition: form-data; name=\"photo\"; filename=\"photo.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            body.writeBytes(bytes); body.writeBytes(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/vehicles/include/member"))
                .timeout(Duration.ofSeconds(30)).header("Authorization", "Bearer owner")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as("Image size %s; response %s", size, response.body())
                .isEqualTo(size == ImageUpload.MAX_BYTES ? 201 : 400);
        }
        verify(storage, times(1)).store(any(), eq("images"));
    }

    @Test void realServletAcceptsFiveImagesAtTheIndividualLimitInOneRequest() throws Exception {
        when(owners.existsById(10L)).thenReturn(true);when(owners.existsActiveById(10L)).thenReturn(true);
        when(parser.parseAndValidate("gallery-http-owner")).thenReturn(new JwtAuthenticatedUser(10L,110L,"Synthetic HTTP gallery actor",Instant.now(clock).plusSeconds(3600)));
        when(authorities.findAuthorityCodesByUserId(10L)).thenReturn(List.of());
        when(storage.store(any(),eq("images"))).thenAnswer(a->new StoredFile("images/2026/10/19/"+UUID.randomUUID()+".png"));
        String data=VehicleMediaOrchestrationIntegrationTest.DATA.replace("MED1D23",VehicleGalleryIntegrationTest.plate()).replace("38249206428",VehicleGalleryIntegrationTest.renavam());
        String boundary="GalleryBoundary"+UUID.randomUUID().toString().replace("-","");var body=new ByteArrayOutputStream();
        body.writeBytes(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"vehicle\"\r\nContent-Type: application/json\r\n\r\n"+data+"\r\n").getBytes(StandardCharsets.UTF_8));
        byte[] image=new byte[ImageUpload.MAX_BYTES];System.arraycopy(VehicleMediaOrchestrationIntegrationTest.PNG,0,image,0,VehicleMediaOrchestrationIntegrationTest.PNG.length);
        for(int i=0;i<5;i++) {
            body.writeBytes(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"photos\"; filename=\"photo.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            body.writeBytes(image);body.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        body.writeBytes(("--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/vehicles/include/member")).header("Authorization","Bearer gallery-http-owner")
                .header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
        var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();var response=client.send(request,HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as("Five files at 5 MiB; response %s",response.body()).isEqualTo(201);verify(storage,times(5)).store(any(),eq("images"));
    }
}
