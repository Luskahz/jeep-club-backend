package com.jeepclub.backend.infra.security.jwt;

import io.jsonwebtoken.security.Keys;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtSigningKeyProvider {

    private final SecretKey key;

    public JwtSigningKeyProvider(@NotNull JwtProperties jwtProperties) {
        this.key = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    public SecretKey getKey() {
        return key;
    }
}