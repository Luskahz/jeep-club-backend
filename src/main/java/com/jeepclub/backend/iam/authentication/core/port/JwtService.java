package com.jeepclub.backend.iam.authentication.core.port;

import com.jeepclub.backend.iam.authentication.core.domain.model.IssuedAccessToken;
import com.jeepclub.backend.iam.authentication.core.domain.model.Session;

public interface JwtService {
    IssuedAccessToken generateAccessToken(Long identityId, Session session);
}
