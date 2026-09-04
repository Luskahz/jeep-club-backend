package com.jeepclub.backend.iam.authentication.infra.integration.memberships;

import com.jeepclub.backend.iam.authentication.core.port.RefreshTokenHashService;
import com.jeepclub.backend.memberships.core.port.MemberActivationTokenHashPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberActivationTokenHashAdapter implements MemberActivationTokenHashPort {

    private final RefreshTokenHashService refreshTokenHashService;

    @Override
    public String hash(String rawToken) {
        return refreshTokenHashService.hash(rawToken);
    }
}
