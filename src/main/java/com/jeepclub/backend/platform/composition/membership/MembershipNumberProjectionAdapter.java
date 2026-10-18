package com.jeepclub.backend.platform.composition.membership;
import com.jeepclub.backend.iam.identity.api.module.spi.UserMembershipNumberProjection;
import com.jeepclub.backend.memberships.api.module.MemberRegistrationQuery;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.*;
@Component @RequiredArgsConstructor
public class MembershipNumberProjectionAdapter implements UserMembershipNumberProjection {
    private final MemberRegistrationQuery members;
    public String currentNumber(Long id) { return members.byIdentityUserId(id).map(r -> r.membershipNumber()).orElse(null); }
    public Map<Long,String> currentNumbers(Collection<Long> ids) {
        var result=new HashMap<Long,String>();var unique=new ArrayList<>(new LinkedHashSet<>(ids));
        for(int i=0;i<unique.size();i+=500) result.putAll(members.currentNumbers(unique.subList(i,Math.min(i+500,unique.size()))));
        return Map.copyOf(result);
    }
}
