package com.jeepclub.backend.iam.authentication.core.port;

public interface PasswordHasher {
    String hash(String raw);
    boolean matches(String raw, String hash);
}
