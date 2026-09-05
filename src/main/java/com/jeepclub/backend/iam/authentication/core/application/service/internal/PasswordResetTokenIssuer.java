package com.jeepclub.backend.iam.authentication.core.application.service.internal;

import com.jeepclub.backend.iam.authentication.core.application.result.IssuedPasswordResetToken;
import com.jeepclub.backend.iam.authentication.core.port.ApplicationUrlProperties;
import com.jeepclub.backend.iam.authentication.core.port.RefreshTokenGenerator;
import com.jeepclub.backend.iam.authentication.core.port.RefreshTokenHashService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenIssuer {
    private final RefreshTokenGenerator tokenGenerator;
    private final RefreshTokenHashService tokenHashService;
    private final ApplicationUrlProperties urlProperties;

    public IssuedPasswordResetToken issue() {
        String rawToken = tokenGenerator.generate();
        return new IssuedPasswordResetToken(
                rawToken,
                tokenHashService.hash(rawToken),
                urlProperties.baseUrl() + "/password-recovery/reset?token=" + rawToken
        );
    }
}
