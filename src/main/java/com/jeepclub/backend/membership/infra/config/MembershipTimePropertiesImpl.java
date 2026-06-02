package com.jeepclub.backend.membership.infra.config;

import com.jeepclub.backend.membership.core.port.MembershipTimeProperties;
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