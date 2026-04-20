package com.jeepclub.backend.infra.security.jwt;

import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtSigningKeyProvider {

    private final JwtProperties jwtProperties;
    @Getter
    private SecretKey key;

    public JwtSigningKeyProvider(@NotNull JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

}