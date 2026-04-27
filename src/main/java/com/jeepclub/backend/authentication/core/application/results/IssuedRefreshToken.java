package com.jeepclub.backend.authentication.core.application.results;

import com.jeepclub.backend.authentication.core.domain.model.RefreshToken;

public record IssuedRefreshToken(
        RefreshToken refreshToken,
        String rawToken
) {}