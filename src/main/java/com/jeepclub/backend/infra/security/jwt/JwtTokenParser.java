package com.jeepclub.backend.infra.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenParser {

    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    // CORRIGIDO AQUI: De "SID" para "sid"
    private static final String SESSION_ID_CLAIM = "sid";

    private final JwtProperties jwtProperties;
    private final JwtSigningKeyProvider keyProvider;

    public JwtAuthenticatedUser parseAndValidate(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("JWT token is required.");
        }

        Claims claims = Jwts.parser()
                .verifyWith(keyProvider.getKey())
                .requireIssuer(jwtProperties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        validateAccessTokenType(claims);

        Long userId = extractUserId(claims);
        Long sessionId = extractSessionId(claims);
        Instant expiresAt = extractExpiration(claims);

        return new JwtAuthenticatedUser(
                userId,
                sessionId,
                expiresAt
        );
    }

    private void validateAccessTokenType(Claims claims) {
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);

        if (!ACCESS_TOKEN_TYPE.equals(tokenType)) {
            throw new IllegalArgumentException("JWT type must be access.");
        }
    }

    private Long extractUserId(Claims claims) {
        String subject = claims.getSubject();

        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("JWT subject is required.");
        }

        String normalizedSubject = subject.trim();

        try {
            Long userId = Long.valueOf(normalizedSubject);

            if (userId <= 0) {
                throw new IllegalArgumentException("JWT subject must be a positive user id.");
            }

            return userId;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("JWT subject must be a valid user id.", exception);
        }
    }

    private Long extractSessionId(Claims claims) {
        Object rawSessionId = claims.get(SESSION_ID_CLAIM);

        if (rawSessionId == null) {
            throw new IllegalArgumentException("JWT session id is required.");
        }

        Long sessionId = convertIntegralNumberToLong(
                rawSessionId,
                "JWT session id must be a valid integer number."
        );

        if (sessionId <= 0) {
            throw new IllegalArgumentException("JWT session id must be positive.");
        }

        return sessionId;
    }

    private Instant extractExpiration(Claims claims) {
        Date expiration = claims.getExpiration();

        if (expiration == null) {
            throw new IllegalArgumentException("JWT expiration is required.");
        }

        Instant expiresAt = expiration.toInstant();

        if (!expiresAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException("JWT is expired.");
        }

        return expiresAt;
    }

    private Long convertIntegralNumberToLong(Object value, String errorMessage) {
        if (value instanceof Byte
                || value instanceof Short
                || value instanceof Integer
                || value instanceof Long) {
            return ((Number) value).longValue();
        }

        if (value instanceof BigInteger bigInteger) {
            try {
                return bigInteger.longValueExact();
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException(errorMessage, exception);
            }
        }

        if (value instanceof BigDecimal bigDecimal) {
            try {
                return bigDecimal.toBigIntegerExact().longValueExact();
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException(errorMessage, exception);
            }
        }

        throw new IllegalArgumentException(errorMessage);
    }
}