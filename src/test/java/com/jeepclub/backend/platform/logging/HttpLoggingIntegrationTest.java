package com.jeepclub.backend.platform.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.jeepclub.backend.iam.identity.api.module.UserAuthenticationTokens;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import com.jeepclub.backend.iam.identity.api.module.UserRegistration;
import com.jeepclub.backend.iam.identity.api.module.UserRegistrationData;
import com.jeepclub.backend.platform.security.jwt.JwtAuthenticatedUser;
import com.jeepclub.backend.platform.security.jwt.JwtProperties;
import com.jeepclub.backend.platform.security.jwt.JwtSigningKeyProvider;
import com.jeepclub.backend.platform.security.jwt.JwtTokenParser;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class HttpLoggingIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRegistration userRegistration;
    @Autowired private UserQuery userQuery;
    @Autowired private Clock clock;
    @Autowired private JwtProperties jwtProperties;
    @Autowired private JwtSigningKeyProvider jwtSigningKeyProvider;
    @Autowired private JwtTokenParser jwtTokenParser;

    private final Logger logger = (Logger) LoggerFactory.getLogger(HttpRequestLogWriter.LOGGER_NAME);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void attachAppender() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
        appender.stop();
        MDC.clear();
    }

    @Test
    @Transactional
    void authenticatedRequestProducesSingleEnrichedOperationalLineAndCleansMdc(
            CapturedOutput output
    ) throws Exception {
        Instant now = Instant.now(clock);
        String cpf = "41876293040";
        UserAuthenticationTokens tokens = userRegistration.registerAndAuthenticate(
                new UserRegistrationData(
                        "Observability User", null, "observability@example.com", cpf,
                        null, null, null, now
                ),
                "observability-password"
        );
        Long userId = userQuery.findByCpf(cpf).orElseThrow().id();
        appender.list.clear();

        mockMvc.perform(get("/identity/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken())
                        .header("X-Request-Id", "integration-authenticated")
                        .header(ClientPlatformResolver.HEADER_NAME, "IOS"))
                .andExpect(status().isOk());

        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.get(0);
        assertThat(event.getFormattedMessage()).contains(
                "http_request",
                "method=GET",
                "path=/identity/me",
                "status=200"
        ).doesNotContain("requestId=", "device=", "userId=", "userName=");
        assertThat(event.getMDCPropertyMap()).containsEntry("requestId", "integration-authenticated")
                .containsEntry("userId", userId.toString())
                .containsEntry("userName", "Observability User")
                .containsEntry("device", "IOS");
        assertCompactConsoleLine(
                httpConsoleLine(output, "integration-authenticated"),
                "integration-authenticated", userId.toString(), "Observability User", "IOS"
        );
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    void anonymousRequestProducesControlledPlaceholdersWithoutResidualIdentity(
            CapturedOutput output
    ) throws Exception {
        mockMvc.perform(post("/authentication/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("X-Request-Id", "integration-anonymous")
                        .header(ClientPlatformResolver.HEADER_NAME, "unsupported"))
                .andExpect(status().isBadRequest());

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getFormattedMessage()).contains(
                "http_request", "method=POST", "status=400"
        ).doesNotContain("requestId=", "device=", "userId=", "userName=");
        assertCompactConsoleLine(
                httpConsoleLine(output, "integration-anonymous"),
                "integration-anonymous", "-", "-", "UNKNOWN"
        );
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    @Transactional
    void legacyTokenWithoutNameKeepsUserIdAndUsesMissingNamePlaceholder(
            CapturedOutput output
    ) throws Exception {
        Instant now = Instant.now(clock);
        String cpf = "16593278008";
        UserAuthenticationTokens tokens = userRegistration.registerAndAuthenticate(
                new UserRegistrationData(
                        "Legacy Observability User", null, "legacy-observability@example.com", cpf,
                        null, null, null, now
                ),
                "legacy-observability-password"
        );
        JwtAuthenticatedUser issued = jwtTokenParser.parseAndValidate(tokens.accessToken());
        String legacyToken = Jwts.builder()
                .setIssuer(jwtProperties.getIssuer())
                .setSubject(issued.userId().toString())
                .claim("typ", "ACCESS")
                .claim("sid", issued.sessionId())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(issued.expiresAt()))
                .signWith(jwtSigningKeyProvider.getKey(), io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();
        appender.list.clear();

        mockMvc.perform(get("/authorization/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + legacyToken)
                        .header("X-Request-Id", "integration-legacy-token")
                        .header(ClientPlatformResolver.HEADER_NAME, "WEB"))
                .andExpect(status().isOk());

        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.get(0);
        assertThat(event.getFormattedMessage()).contains(
                "http_request", "method=GET", "path=/authorization/me", "status=200"
        ).doesNotContain(
                "requestId=", "device=", "userId=", "userName=",
                "legacy-observability@example.com"
        );
        assertThat(event.getMDCPropertyMap())
                .containsEntry("requestId", "integration-legacy-token")
                .containsEntry("userId", issued.userId().toString())
                .containsEntry("device", "WEB")
                .doesNotContainKey("userName");
        assertCompactConsoleLine(
                httpConsoleLine(output, "integration-legacy-token"),
                "integration-legacy-token", issued.userId().toString(), "-", "WEB"
        );
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    private String httpConsoleLine(CapturedOutput output, String requestId) {
        return output.getAll().lines()
                .filter(line -> line.contains("requestId=" + requestId))
                .filter(line -> line.contains(" - http_request "))
                .findFirst()
                .orElseThrow(() -> new AssertionError("HTTP console line not found for " + requestId));
    }

    private void assertCompactConsoleLine(
            String line,
            String requestId,
            String userId,
            String userName,
            String device
    ) {
        assertThat(line).contains(
                "requestId=" + requestId,
                "userId=" + userId,
                "userName=\"" + userName + "\"",
                "device=" + device,
                "http_request method=",
                "path=",
                "status=",
                "durationMs="
        ).doesNotContain("\n", "\r");
        assertThat(occurrences(line, "requestId=")).isEqualTo(1);
        assertThat(occurrences(line, "userId=")).isEqualTo(1);
        assertThat(occurrences(line, "userName=")).isEqualTo(1);
        assertThat(occurrences(line, "device=")).isEqualTo(1);
        assertThat(occurrences(line, "method=")).isEqualTo(1);
        assertThat(occurrences(line, "path=")).isEqualTo(1);
        assertThat(occurrences(line, "status=")).isEqualTo(1);
        assertThat(occurrences(line, "durationMs=")).isEqualTo(1);
    }

    private int occurrences(String value, String fragment) {
        return (value.length() - value.replace(fragment, "").length()) / fragment.length();
    }
}
