package com.jeepclub.backend.authentication.core.port;


public interface TokenHashService {
    String hash(String rawToken);
}
