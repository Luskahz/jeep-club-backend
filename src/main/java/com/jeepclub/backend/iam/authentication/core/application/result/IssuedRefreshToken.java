package com.jeepclub.backend.iam.authentication.core.application.result;

import com.jeepclub.backend.iam.authentication.core.domain.model.RefreshToken;

public record IssuedRefreshToken(
        RefreshToken refreshToken,
        String rawToken
) {}