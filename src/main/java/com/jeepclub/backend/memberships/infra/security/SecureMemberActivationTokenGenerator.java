package com.jeepclub.backend.memberships.infra.security;

import com.jeepclub.backend.memberships.core.port.MemberActivationTokenGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SecureMemberActivationTokenGenerator implements MemberActivationTokenGenerator {

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        byte[] bytes = new byte[64];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
