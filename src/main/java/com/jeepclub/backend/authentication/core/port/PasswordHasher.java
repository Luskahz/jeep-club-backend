package com.jeepclub.backend.authentication.core.port;

public interface PasswordHasher {
    String hash(String raw);
    boolean matches(String raw, String hash);
}
