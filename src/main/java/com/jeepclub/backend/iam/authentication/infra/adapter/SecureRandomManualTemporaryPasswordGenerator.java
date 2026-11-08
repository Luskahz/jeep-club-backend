package com.jeepclub.backend.iam.authentication.infra.adapter;

import com.jeepclub.backend.iam.authentication.core.port.ManualTemporaryPasswordGenerator;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class SecureRandomManualTemporaryPasswordGenerator implements ManualTemporaryPasswordGenerator {
    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        StringBuilder password = new StringBuilder(8);
        for (int i = 0; i < 5; i++) password.append((char) ('a' + random.nextInt(26)));
        for (int i = 0; i < 3; i++) password.append((char) ('0' + random.nextInt(10)));
        return password.toString();
    }
}
