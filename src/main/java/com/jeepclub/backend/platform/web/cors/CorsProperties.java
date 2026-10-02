package com.jeepclub.backend.platform.web.cors;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    @NotEmpty
    private List<@NotBlank String> allowedOrigins;

    @NotEmpty
    private List<@NotBlank String> allowedMethods;

    @NotEmpty
    private List<@NotBlank String> allowedHeaders;

    @NotNull
    private Boolean allowCredentials;

    @NotNull
    @PositiveOrZero
    private Long maxAge;

    public List<String> getAllowedOrigins() { return allowedOrigins; }
    public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    public List<String> getAllowedMethods() { return allowedMethods; }
    public void setAllowedMethods(List<String> allowedMethods) { this.allowedMethods = allowedMethods; }
    public List<String> getAllowedHeaders() { return allowedHeaders; }
    public void setAllowedHeaders(List<String> allowedHeaders) { this.allowedHeaders = allowedHeaders; }
    public Boolean getAllowCredentials() { return allowCredentials; }
    public void setAllowCredentials(Boolean allowCredentials) { this.allowCredentials = allowCredentials; }
    public Long getMaxAge() { return maxAge; }
    public void setMaxAge(Long maxAge) { this.maxAge = maxAge; }
}
