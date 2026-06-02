package com.jeepclub.backend.memberships.infra.config;

import com.jeepclub.backend.memberships.core.port.MembershipTimeProperties;
import jakarta.validation.constraints.NotNull;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Component
@Validated
@Setter
@ConfigurationProperties(prefix = "membership")
public class MembershipTimePropertiesImpl implements MembershipTimeProperties {

    @NotNull
    private Duration activationTokenTtl;

    @Override
    public Duration activationTokenTtl() {
        return activationTokenTtl;
    }
}