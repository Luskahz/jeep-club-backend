package com.jeepclub.backend.authentication.infra.security.jwt;

import com.jeepclub.backend.authentication.core.domain.model.IssuedAccessToken;
import com.jeepclub.backend.authentication.core.domain.model.Session;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.port.JwtService;
import com.jeepclub.backend.infra.security.jwt.JwtProperties;
import com.jeepclub.backend.infra.security.jwt.JwtSigningKeyProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    private final JwtProperties jwtProperties;
    private final JwtSigningKeyProvider keyProvider;

    @Override
    public IssuedAccessToken generateAccessToken(@NotNull User user, @NotNull Session session) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.getAccessTokenExpirationSeconds());

        String token = Jwts.builder()
                .setIssuer(jwtProperties.getIssuer())
                .setSubject(user.getId().toString())
                .claim("typ", "access")
                .claim("sid", session.getId())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiresAt))
                .signWith(keyProvider.getKey(), SignatureAlgorithm.HS256)
                .compact();

        return new IssuedAccessToken(token, expiresAt);
    }
}