package com.jeepclub.backend.iam.authentication.core.port;


public interface RefreshTokenHashService {
    String hash(String rawToken);
}
