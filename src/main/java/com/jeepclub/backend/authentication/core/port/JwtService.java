package com.jeepclub.backend.authentication.core.port;

import com.jeepclub.backend.authentication.core.domain.model.IssuedAccessToken;
import com.jeepclub.backend.authentication.core.domain.model.Session;

public interface JwtService {
    IssuedAccessToken generateAccessToken(Long identityId, Session session);
}
