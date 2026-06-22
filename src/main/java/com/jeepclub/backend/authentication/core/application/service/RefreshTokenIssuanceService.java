package com.jeepclub.backend.authentication.core.application.service;

import com.jeepclub.backend.authentication.core.application.result.IssuedRefreshToken;
import com.jeepclub.backend.authentication.core.domain.model.RefreshToken;
import com.jeepclub.backend.authentication.core.domain.model.Session;
import com.jeepclub.backend.authentication.core.port.ApplicationTimeProperties;
import com.jeepclub.backend.authentication.core.port.RefreshTokenGenerator;
import com.jeepclub.backend.authentication.core.port.RefreshTokenHashService;
import com.jeepclub.backend.authentication.core.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RefreshTokenIssuanceService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenHashService tokenHashService;
    private final RefreshTokenGenerator tokenGenerator;
    private final ApplicationTimeProperties timeProperties;

    public IssuedRefreshToken issue(Session session, Instant now) {
        String rawToken = tokenGenerator.generate();
        RefreshToken saved = refreshTokenRepository.save(RefreshToken.create(
                session,
                tokenHashService.hash(rawToken),
                timeProperties.refreshTokenTtl(),
                now
        ));
        return new IssuedRefreshToken(saved, rawToken);
    }
}
