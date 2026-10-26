package com.jeepclub.backend.memberships.infra.config;
import com.jeepclub.backend.memberships.core.port.MemberIssuancePolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
@Component @ConfigurationProperties(prefix="memberships.registry.issuance") @Getter @Setter
public class MemberIssuanceProperties implements MemberIssuancePolicy {
    private boolean enabled;
    private boolean approved;
    private Long initialNumber;
    private Integer width;
    private String prefix;
    @PostConstruct public void validate() {
        // Retain binding of retired settings to reject stale deployments explicitly.
        if (initialNumber != null || width != null || (prefix != null && !prefix.isEmpty())) {
            throw new IllegalStateException("Membership issuance initial-number, width and prefix are retired; remove legacy format configuration.");
        }
        if (enabled && !approved) throw new IllegalStateException("Membership issuance requires explicit operational approval.");
    }
    public boolean enabled() { return enabled; }
}
