package com.jeepclub.backend.authentication.core.application.service.internal;

import com.jeepclub.backend.authentication.core.application.result.login.PasswordChangeRequiredLoginResult;
import com.jeepclub.backend.authentication.core.domain.model.PasswordChangeChallenge;
import com.jeepclub.backend.authentication.core.port.ApplicationTimeProperties;
import com.jeepclub.backend.authentication.core.port.RefreshTokenGenerator;
import com.jeepclub.backend.authentication.core.port.RefreshTokenHashService;
import com.jeepclub.backend.authentication.core.repository.PasswordChangeChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class PasswordChangeChallengeIssuer {

    private final PasswordChangeChallengeRepository challengeRepository;
    private final RefreshTokenGenerator tokenGenerator;
    private final RefreshTokenHashService tokenHashService;
    private final ApplicationTimeProperties timeProperties;

    public PasswordChangeRequiredLoginResult issue(Long userId, Instant now) {
        challengeRepository.invalidateActiveByUserId(userId, now);
        String rawToken = tokenGenerator.generate();
        Instant expiresAt = now.plus(timeProperties.passwordChangeChallengeTtl());
        challengeRepository.save(PasswordChangeChallenge.create(
                userId,
                tokenHashService.hash(rawToken),
                now,
                expiresAt
        ));
        return new PasswordChangeRequiredLoginResult(rawToken, expiresAt);
    }
}
