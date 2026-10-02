package com.jeepclub.backend.memberships.infra.config;

import com.jeepclub.backend.memberships.core.port.MembershipActivationUrlProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@Setter
@ConfigurationProperties(prefix = "app.user-facing")
public class MembershipActivationUrlPropertiesImpl implements MembershipActivationUrlProperties {
    @NotBlank
    private String membershipActivationUrl;

    @Override
    public String activationUrl() {
        return membershipActivationUrl;
    }
}
