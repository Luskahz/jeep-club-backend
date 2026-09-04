package com.jeepclub.backend.authentication.infra.security.jwt;

import com.jeepclub.backend.iam.authentication.core.domain.enums.SessionStatus;
import com.jeepclub.backend.iam.authentication.core.domain.model.IssuedAccessToken;
import com.jeepclub.backend.iam.authentication.core.domain.model.Session;
import com.jeepclub.backend.iam.authentication.infra.security.jwt.JwtServiceImpl;
import com.jeepclub.backend.platform.security.jwt.JwtAuthenticatedUser;
import com.jeepclub.backend.platform.security.jwt.JwtProperties;
import com.jeepclub.backend.platform.security.jwt.JwtSigningKeyProvider;
import com.jeepclub.backend.platform.security.jwt.JwtTokenParser;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceImplTest {

    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    @Test
    void usesStableIdentityIdAsSubjectWithoutDependingOnUserAggregate() {
        JwtProperties properties = properties();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        JwtSigningKeyProvider keyProvider = new JwtSigningKeyProvider(properties);
        JwtServiceImpl service = new JwtServiceImpl(properties, keyProvider, clock);
        JwtTokenParser parser = new JwtTokenParser(properties, keyProvider, clock);
        Session session = Session.reconstitute(
                7L,
                42L,
                NOW,
                NOW.plusSeconds(3600),
                null,
                SessionStatus.ACTIVE
        );

        IssuedAccessToken token = service.generateAccessToken(42L, session);
        JwtAuthenticatedUser authenticated = parser.parseAndValidate(token.token());

        assertThat(authenticated.userId()).isEqualTo(42L);
        assertThat(authenticated.sessionId()).isEqualTo(7L);
        assertThat(token.expiresAt()).isEqualTo(NOW.plusSeconds(900));
    }

    private JwtProperties properties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("a-secure-test-secret-with-at-least-32-bytes");
        properties.setAccessTokenExpirationSeconds(900);
        properties.setIssuer("jeep-club-test");
        return properties;
    }
}
