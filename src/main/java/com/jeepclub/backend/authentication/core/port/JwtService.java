package com.jeepclub.backend.authentication.core.port;

import com.jeepclub.backend.authentication.core.domain.model.IssuedAccessToken;
import com.jeepclub.backend.authentication.core.domain.model.Session;
import com.jeepclub.backend.authentication.core.domain.model.User;

public interface JwtService {
    IssuedAccessToken generateAccessToken(User user, Session session);
}
