package com.jeepclub.backend.iam.authentication.infra.config.security;


import com.jeepclub.backend.iam.authentication.core.port.ApplicationUrlProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@Setter
@ConfigurationProperties(prefix = "app.server")
public class ApplicationUrlPropertiesImpl implements ApplicationUrlProperties {

    @NotBlank
    private String baseUrl;

    @Override
    public String baseUrl() {
        return baseUrl;
    }
}
