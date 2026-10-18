package com.jeepclub.backend.platform.composition.membership;
import com.jeepclub.backend.iam.identity.api.module.spi.UserMembershipNumberProjection;
import com.jeepclub.backend.memberships.api.module.MemberRegistrationQuery;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.*;
@Component @RequiredArgsConstructor
public class MembershipNumberProjectionAdapter implements UserMembershipNumberProjection {
    private final MemberRegistrationQuery members;
    public Optional<Long> identityIdByCurrentNumber(String number) {
        if (number == null || number.isBlank() || number.length() > 64
                || Character.isWhitespace(number.codePointAt(0)) || Character.isWhitespace(number.codePointBefore(number.length()))
                || number.codePoints().anyMatch(c -> Character.isISOControl(c) || Character.getType(c) == Character.FORMAT || Character.getType(c) == Character.SURROGATE)) return Optional.empty();
        return members.byCurrentMembershipNumber(number).map(r -> r.identityUserId());
    }
    public String currentNumber(Long id) { return members.byIdentityUserId(id).map(r -> r.membershipNumber()).orElse(null); }
    public Map<Long,String> currentNumbers(Collection<Long> ids) {
        var result=new HashMap<Long,String>();var unique=new ArrayList<>(new LinkedHashSet<>(ids));
        for(int i=0;i<unique.size();i+=500) result.putAll(members.currentNumbers(unique.subList(i,Math.min(i+500,unique.size()))));
        return Map.copyOf(result);
    }
}
