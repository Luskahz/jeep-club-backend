package com.jeepclub.backend.authentication.core.port;


public interface RefreshTokenHashService {
    String hash(String rawToken);
}
