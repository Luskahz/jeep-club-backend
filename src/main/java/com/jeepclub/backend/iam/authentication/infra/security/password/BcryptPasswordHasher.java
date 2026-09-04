package com.jeepclub.backend.iam.authentication.infra.security.password;

import com.jeepclub.backend.iam.authentication.core.port.PasswordHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BcryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder passwordEncoder;

    @Override
    public String hash(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }

        return passwordEncoder.encode(raw);
    }

    @Override
    public boolean matches(String raw, String hash) {
        if (raw == null || raw.isBlank()) {
            return false;
        }

        if (hash == null || hash.isBlank()) {
            return false;
        }

        return passwordEncoder.matches(raw, hash);
    }
}