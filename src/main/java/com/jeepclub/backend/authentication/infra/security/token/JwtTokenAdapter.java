package com.jeepclub.backend.authentication.infra.security.token;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.dtos.UserPayload;
import com.jeepclub.backend.authentication.core.services.TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;

@Component
public class JwtTokenAdapter implements TokenService {

    private final String CLAIM_USER_ID = "id";
    private final String CLAIM_NAME = "name";
    private final String CLAIM_TOKEN_TYPE = "type";
    private final String TOKEN_TYPE_ACCESS = "access";
    private final String TOKEN_TYPE_REFRESH = "refresh";

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final String issuer;
    private final long accessTokenExpirationMinutes;
    private final long refreshTokenExpirationDays;

    public JwtTokenAdapter(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.issuer:jeep-club-backend}") String issuer,
            @Value("${security.jwt.access-token-expiration-minutes:15}") long accessTokenExpirationMinutes,
            @Value("${security.jwt.refresh-token-expiration-days:7}") long refreshTokenExpirationDays) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("security.jwt.secret must be configured");
        }
        this.algorithm = Algorithm.HMAC256(secret);
        this.issuer = Objects.requireNonNullElse(issuer, "jeep-club-backend");
        this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
        this.verifier = JWT.require(this.algorithm)
                .withIssuer(this.issuer)
                .build();
    }


    @Override
    public String generateAccessToken(User domainUser) {
        return createToken(domainUser, TOKEN_TYPE_ACCESS, Instant.now().plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES));
    }

    @Override
    public String generateRefreshToken(User domainUser) {
        return createToken(domainUser, TOKEN_TYPE_REFRESH, Instant.now().plus(refreshTokenExpirationDays, ChronoUnit.DAYS));
    }

    @Override
    public boolean validateToken(String token) {
        try {
            verifyAndDecode(token);
            return true;
        } catch (JWTVerificationException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public UserPayload getPayload(String token) {
        DecodedJWT decodedJWT = verifyAndDecode(token);
        Long userId = decodedJWT.getClaim(CLAIM_USER_ID).asLong();
        String name = decodedJWT.getClaim(CLAIM_NAME).asString();

        if (userId == null || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Token payload is missing required user data");
        }

        return new UserPayload(userId, name);
    }

    private String createToken(User domainUser, String tokenType, Instant expiration) {
        if (domainUser == null || domainUser.getId() == null || domainUser.getName() == null || domainUser.getName().isBlank()) {
            throw new IllegalArgumentException("User must contain id and name to generate a token");
        }

        Instant now = Instant.now();

        return JWT.create()
                .withIssuer(issuer)
                .withSubject(String.valueOf(domainUser.getId()))
                .withClaim(CLAIM_USER_ID, domainUser.getId())
                .withClaim(CLAIM_NAME, domainUser.getName())
                .withClaim(CLAIM_TOKEN_TYPE, tokenType)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiration))
                .sign(algorithm);
    }

    private DecodedJWT verifyAndDecode(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token must not be null or blank");
        }
        return verifier.verify(token);
    }
}
