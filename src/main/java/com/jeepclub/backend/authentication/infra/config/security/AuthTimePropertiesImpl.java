package com.jeepclub.backend.authentication.infra.config.security;

import com.jeepclub.backend.authentication.core.port.AuthTimeProperties;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Component
@Setter
@Getter
@Validated
@ConfigurationProperties(prefix = "security.auth")
public class AuthTimePropertiesImpl implements AuthTimeProperties {

    @NotNull
    private Duration refreshTokenTtl;

    @NotNull
    private Duration sessionTtl;

    @Override
    public Duration sessionTtl() {
        return sessionTtl;
    }

    @Override
    public Duration refreshTokenTtl() {
        return refreshTokenTtl;
    }

}