package com.jeepclub.backend.membership.core.application.service;

import com.jeepclub.backend.authentication.core.port.RefreshTokenHashService;
import com.jeepclub.backend.membership.core.application.exception.MemberActivationTokenNotFoundException;
import com.jeepclub.backend.membership.core.domain.model.MemberActivationToken;
import com.jeepclub.backend.membership.core.repository.MemberActivationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ValidateActivationTokenService {

    private final MemberActivationTokenRepository memberActivationTokenRepository;
    private final RefreshTokenHashService tokenHashService;
    private final Clock clock;

    @Transactional
    public Long validate(String rawToken) {
        Instant now = Instant.now(clock);
        String tokenHash = tokenHashService.hash(rawToken);

        MemberActivationToken token = memberActivationTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(MemberActivationTokenNotFoundException::new);

        token.validateOrThrow(now);

        token.markAsUsed(now);
        memberActivationTokenRepository.save(token);

        return token.getApplicationId();
    }
}