package com.jeepclub.backend.infra.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class JwtTokenParser {

    private final JwtProperties jwtProperties;
    private final JwtSigningKeyProvider keyProvider;

    public JwtAuthenticatedUser parseAndValidate(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(keyProvider.getKey())
                .requireIssuer(jwtProperties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (claims.getSubject() == null || claims.getSubject().isBlank()) {
            throw new IllegalArgumentException("JWT subject is required.");
        }

        Long userId = Long.valueOf(claims.getSubject());

        Number sidNum = claims.get("sid", Number.class);

        if (sidNum == null) {
            throw new IllegalArgumentException("JWT session id is required.");
        }

        Long sessionId = sidNum.longValue();

        Instant expiresAt = claims.getExpiration().toInstant();

        return new JwtAuthenticatedUser(userId, sessionId, expiresAt);
    }
}