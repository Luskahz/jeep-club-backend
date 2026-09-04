package com.jeepclub.backend.iam.authentication.core.application.service.internal;

import com.jeepclub.backend.iam.authentication.core.application.result.IssuedTemporaryPassword;
import com.jeepclub.backend.iam.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.iam.authentication.core.port.RandomPasswordGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TemporaryPasswordIssuer {
    private final RandomPasswordGenerator passwordGenerator;
    private final PasswordHasher passwordHasher;

    public IssuedTemporaryPassword issue() {
        String rawPassword = passwordGenerator.generateSecurePassword();
        return new IssuedTemporaryPassword(rawPassword, passwordHasher.hash(rawPassword));
    }
}
