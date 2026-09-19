package com.jeepclub.backend.iam.authentication.infra.security.token;

import com.jeepclub.backend.iam.authentication.core.port.RefreshTokenGenerator;
import com.jeepclub.backend.memberships.core.port.MemberActivationTokenGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SecureRandomTokenGenerator implements RefreshTokenGenerator, MemberActivationTokenGenerator {

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        byte[] bytes = new byte[64];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
