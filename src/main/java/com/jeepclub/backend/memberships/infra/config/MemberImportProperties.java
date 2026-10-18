package com.jeepclub.backend.memberships.infra.config;
import com.jeepclub.backend.memberships.core.port.MemberImportApplicationPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
@Component
@ConfigurationProperties(prefix="memberships.registry.imports")
public class MemberImportProperties implements MemberImportApplicationPolicy {
    private boolean applyEnabled;
    public boolean isApplyEnabled() { return applyEnabled; }
    public void setApplyEnabled(boolean enabled) { this.applyEnabled=enabled; }
    @Override public boolean applyEnabled() { return applyEnabled; }
}
