package com.jeepclub.backend.iam.authentication.infra.security.jwt;

import com.jeepclub.backend.iam.authentication.core.domain.model.IssuedAccessToken;
import com.jeepclub.backend.iam.authentication.core.domain.model.Session;
import com.jeepclub.backend.iam.authentication.core.port.JwtService;
import com.jeepclub.backend.platform.security.jwt.JwtProperties;
import com.jeepclub.backend.platform.security.jwt.JwtSigningKeyProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    private final JwtProperties jwtProperties;
    private final JwtSigningKeyProvider keyProvider;
    private final Clock clock;

    @Override
    public IssuedAccessToken generateAccessToken(
            @NotNull Long identityId,
            @NotNull Session session
    ) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plusSeconds(jwtProperties.getAccessTokenExpirationSeconds());

        String token = Jwts.builder()
                .setIssuer(jwtProperties.getIssuer())
                .setSubject(identityId.toString())
                .claim("typ", "ACCESS")
                .claim("sid", session.getId())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiresAt))
                .signWith(keyProvider.getKey(), SignatureAlgorithm.HS256)
                .compact();

        return new IssuedAccessToken(token, expiresAt);
    }
}
