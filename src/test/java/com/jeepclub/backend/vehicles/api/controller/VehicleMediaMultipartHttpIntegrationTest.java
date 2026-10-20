package com.jeepclub.backend.vehicles.api.controller;

import com.jeepclub.backend.iam.authentication.core.application.service.security.AccessTokenAuthenticationService;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.platform.security.jwt.JwtAuthenticatedUser;
import com.jeepclub.backend.platform.security.jwt.JwtTokenParser;
import com.jeepclub.backend.platform.storage.image.ImageUpload;
import com.jeepclub.backend.shared.storage.FileStorage;
import com.jeepclub.backend.shared.storage.StoredFile;
import org.junit.jupiter.api.Test;
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
    @MockitoBean FileStorage storage;
    @MockitoBean JwtTokenParser parser;
    @MockitoBean UserAuthoritiesProvider authorities;
    @MockitoBean AccessTokenAuthenticationService authentication;

    @Test void realServletAcceptsFiveMbImageAndRejectsOneExtraByte() throws Exception {
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
}
