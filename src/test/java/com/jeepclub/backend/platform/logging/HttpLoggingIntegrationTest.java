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
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
    void authenticatedRequestProducesSingleEnrichedOperationalLineAndCleansMdc() throws Exception {
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
                "requestId=integration-authenticated",
                "method=GET",
                "path=/identity/me",
                "status=200",
                "device=IOS",
                "userId=" + userId,
                "userName=\"Observability User\""
        );
        assertThat(event.getMDCPropertyMap()).containsEntry("requestId", "integration-authenticated")
                .containsEntry("userId", userId.toString())
                .containsEntry("userName", "Observability User")
                .containsEntry("device", "IOS");
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    void anonymousRequestProducesControlledPlaceholdersWithoutResidualIdentity() throws Exception {
        mockMvc.perform(post("/authentication/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("X-Request-Id", "integration-anonymous")
                        .header(ClientPlatformResolver.HEADER_NAME, "unsupported"))
                .andExpect(status().isBadRequest());

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getFormattedMessage()).contains(
                "requestId=integration-anonymous",
                "status=400",
                "device=UNKNOWN",
                "userId=-",
                "userName=\"-\""
        );
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    @Transactional
    void legacyTokenWithoutNameKeepsUserIdAndUsesMissingNamePlaceholder() throws Exception {
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
                "requestId=integration-legacy-token",
                "userId=" + issued.userId(),
                "userName=\"-\""
        ).doesNotContain("legacy-observability@example.com");
        assertThat(event.getMDCPropertyMap())
                .containsEntry("requestId", "integration-legacy-token")
                .containsEntry("userId", issued.userId().toString())
                .containsEntry("device", "WEB")
                .doesNotContainKey("userName");
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }
}
