package com.jeepclub.backend.memberships.infra.config;
import com.jeepclub.backend.memberships.core.port.MemberIssuancePolicy;
import com.jeepclub.backend.memberships.core.domain.model.MembershipNumber;
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
        if (enabled && (!approved || initialNumber==null || initialNumber<=0 || initialNumber==Long.MAX_VALUE
                || width==null || width<1 || width>64 || prefix==null || prefix.length()+Math.max(width,19)>64)) {
            throw new IllegalStateException("Membership issuance requires approved range and explicit format.");
        }
        if(enabled) { new MembershipNumber(format(initialNumber)); new MembershipNumber(format(Long.MAX_VALUE)); }
    }
    public boolean enabled() { return enabled; }
    public long initialNumber() { if (!enabled) throw new IllegalStateException("Membership issuance disabled."); return initialNumber; }
    public String format(long n) { return prefix + "0".repeat(Math.max(0,width-Long.toString(n).length())) + n; }
    public String signature() { return "v1:"+initialNumber+":"+width+":"+prefix; }
}
