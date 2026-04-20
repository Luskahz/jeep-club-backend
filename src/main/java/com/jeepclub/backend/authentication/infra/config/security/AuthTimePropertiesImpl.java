package com.jeepclub.backend.authentication.infra.config.security;

import com.jeepclub.backend.authentication.core.port.AuthTimeProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Setter
@Getter
@ConfigurationProperties(prefix = "security.auth")
public class AuthTimePropertiesImpl implements AuthTimeProperties {
    private Duration refreshTokenTtl;
    private Duration passwordChangeRequestTtl;
    private Duration sessionTtl;

    @Override
    public Duration sessionTtl() {
        return sessionTtl;
    }

    @Override
    public Duration refreshTokenTtl() {
        return refreshTokenTtl;
    }

    @Override
    public Duration passwordChangeRequestTtl() {
        return passwordChangeRequestTtl;
    }
}
