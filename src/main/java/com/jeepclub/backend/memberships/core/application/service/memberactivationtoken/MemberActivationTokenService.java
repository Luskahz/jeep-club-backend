package com.jeepclub.backend.memberships.core.application.service.memberactivationtoken;

import com.jeepclub.backend.memberships.core.application.exception.MemberActivationTokenNotFoundException;
import com.jeepclub.backend.memberships.core.domain.model.MemberActivationToken;
import com.jeepclub.backend.memberships.core.port.MemberActivationTokenHashPort;
import com.jeepclub.backend.memberships.core.repository.MemberActivationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MemberActivationTokenService {

    private final MemberActivationTokenRepository memberActivationTokenRepository;
    private final MemberActivationTokenHashPort tokenHashPort;
    private final Clock clock;

    @Transactional
    public Long validate(String rawToken) {
        Instant now = Instant.now(clock);
        String tokenHash = tokenHashPort.hash(rawToken);

        MemberActivationToken token = memberActivationTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(MemberActivationTokenNotFoundException::new);

        token.validateOrThrow(now);
        token.markAsUsed(now);
        memberActivationTokenRepository.save(token);

        return token.getApplicationId();
    }
}
