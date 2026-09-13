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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

        IssuedAccessToken token = service.generateAccessToken(42L, "Lucas Alves", session);
        JwtAuthenticatedUser authenticated = parser.parseAndValidate(token.token());

        assertThat(authenticated.userId()).isEqualTo(42L);
        assertThat(authenticated.sessionId()).isEqualTo(7L);
        assertThat(authenticated.userName()).isEqualTo("Lucas Alves");
        assertThat(token.expiresAt()).isEqualTo(NOW.plusSeconds(900));
    }

    @Test
    void acceptsLegacyAccessTokenWithoutObservationalNameClaim() {
        JwtProperties properties = properties();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        JwtSigningKeyProvider keyProvider = new JwtSigningKeyProvider(properties);
        JwtTokenParser parser = new JwtTokenParser(properties, keyProvider, clock);
        String token = io.jsonwebtoken.Jwts.builder()
                .setIssuer(properties.getIssuer())
                .setSubject("42")
                .claim("typ", "ACCESS")
                .claim("sid", 7L)
                .setIssuedAt(java.util.Date.from(NOW))
                .setExpiration(java.util.Date.from(NOW.plusSeconds(900)))
                .signWith(keyProvider.getKey(), io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();

        JwtAuthenticatedUser authenticated = parser.parseAndValidate(token);

        assertThat(authenticated.userId()).isEqualTo(42L);
        assertThat(authenticated.sessionId()).isEqualTo(7L);
        assertThat(authenticated.userName()).isNull();
        assertThat(authenticated.expiresAt()).isEqualTo(NOW.plusSeconds(900));
    }

    @Test
    void rejectsPresentButBlankObservationalNameClaim() {
        JwtProperties properties = properties();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        JwtSigningKeyProvider keyProvider = new JwtSigningKeyProvider(properties);
        JwtTokenParser parser = new JwtTokenParser(properties, keyProvider, clock);
        String token = io.jsonwebtoken.Jwts.builder()
                .setIssuer(properties.getIssuer())
                .setSubject("42")
                .claim("typ", "ACCESS")
                .claim("sid", 7L)
                .claim("name", "   ")
                .setIssuedAt(java.util.Date.from(NOW))
                .setExpiration(java.util.Date.from(NOW.plusSeconds(900)))
                .signWith(keyProvider.getKey(), io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();

        assertThatThrownBy(() -> parser.parseAndValidate(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JWT user name must not be blank.");
    }

    @Test
    void keepsRejectingExpiredAccessToken() {
        JwtProperties properties = properties();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        JwtSigningKeyProvider keyProvider = new JwtSigningKeyProvider(properties);
        JwtTokenParser parser = new JwtTokenParser(properties, keyProvider, clock);
        String token = io.jsonwebtoken.Jwts.builder()
                .setIssuer(properties.getIssuer())
                .setSubject("42")
                .claim("typ", "ACCESS")
                .claim("sid", 7L)
                .setExpiration(java.util.Date.from(NOW.minusSeconds(1)))
                .signWith(keyProvider.getKey(), io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();

        assertThatThrownBy(() -> parser.parseAndValidate(token))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    @Test
    void keepsRejectingTokenWithInvalidSignature() {
        JwtProperties properties = properties();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        JwtSigningKeyProvider keyProvider = new JwtSigningKeyProvider(properties);
        JwtProperties otherProperties = properties();
        otherProperties.setSecret("another-secure-test-secret-with-at-least-32-bytes");
        JwtSigningKeyProvider otherKeyProvider = new JwtSigningKeyProvider(otherProperties);
        JwtTokenParser parser = new JwtTokenParser(properties, keyProvider, clock);
        String token = io.jsonwebtoken.Jwts.builder()
                .setIssuer(properties.getIssuer())
                .setSubject("42")
                .claim("typ", "ACCESS")
                .claim("sid", 7L)
                .setExpiration(java.util.Date.from(NOW.plusSeconds(900)))
                .signWith(otherKeyProvider.getKey(), io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();

        assertThatThrownBy(() -> parser.parseAndValidate(token))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    @Test
    void keepsRejectingNonAccessTokenType() {
        JwtProperties properties = properties();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        JwtSigningKeyProvider keyProvider = new JwtSigningKeyProvider(properties);
        JwtTokenParser parser = new JwtTokenParser(properties, keyProvider, clock);
        String token = io.jsonwebtoken.Jwts.builder()
                .setIssuer(properties.getIssuer())
                .setSubject("42")
                .claim("typ", "REFRESH")
                .claim("sid", 7L)
                .setExpiration(java.util.Date.from(NOW.plusSeconds(900)))
                .signWith(keyProvider.getKey(), io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();

        assertThatThrownBy(() -> parser.parseAndValidate(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JWT type must be access.");
    }

    private JwtProperties properties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("a-secure-test-secret-with-at-least-32-bytes");
        properties.setAccessTokenExpirationSeconds(900);
        properties.setIssuer("jeep-club-test");
        return properties;
    }
}
