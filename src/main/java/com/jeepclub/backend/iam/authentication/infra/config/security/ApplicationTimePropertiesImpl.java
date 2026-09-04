package com.jeepclub.backend.iam.authentication.infra.config.security;

import com.jeepclub.backend.iam.authentication.core.port.ApplicationTimeProperties;
import jakarta.validation.constraints.NotNull;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Component
@Validated
@Setter
@ConfigurationProperties(prefix = "security.auth")
public class ApplicationTimePropertiesImpl implements ApplicationTimeProperties {

    @NotNull
    private Duration refreshTokenTtl;

    @NotNull
    private Duration sessionTtl;

    @NotNull
    private Duration passwordRecoveryRequestTtl;

    @NotNull
    private Duration passwordChangeChallengeTtl;

    @Override
    public Duration sessionTtl() {
        return sessionTtl;
    }

    @Override
    public Duration refreshTokenTtl() {
        return refreshTokenTtl;
    }

    @Override
    public Duration passwordRecoveryRequestTtl() {
        return passwordRecoveryRequestTtl;
    }

    @Override
    public Duration passwordChangeChallengeTtl() {
        return passwordChangeChallengeTtl;
    }


}