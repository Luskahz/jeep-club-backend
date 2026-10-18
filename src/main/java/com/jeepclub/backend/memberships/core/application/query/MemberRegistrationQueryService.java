package com.jeepclub.backend.memberships.core.application.query;
import com.jeepclub.backend.memberships.api.module.*;
import com.jeepclub.backend.memberships.core.repository.MemberRepository;
import com.jeepclub.backend.memberships.core.domain.model.MembershipNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class MemberRegistrationQueryService implements MemberRegistrationQuery {
    private final MemberRepository members;
    public Optional<MemberRegistration> byIdentityUserId(Long id) {
        return members.byIdentity(id).map(m -> new MemberRegistration(m.id(),m.identityUserId(),m.membershipNumber()));
    }
    public Map<Long,String> currentNumbers(Collection<Long> ids) {
        if (ids == null || ids.size()>500 || ids.stream().anyMatch(id -> id==null || id<=0)) throw new IllegalArgumentException("Invalid membership lookup batch.");
        return members.byIdentities(new HashSet<>(ids)).stream().collect(Collectors.toUnmodifiableMap(m -> m.identityUserId(), m -> m.membershipNumber()));
    }
    public Optional<MemberRegistration> byCurrentMembershipNumber(String number) {
        new MembershipNumber(number);
        return members.byNumber(number).map(m -> new MemberRegistration(m.id(),m.identityUserId(),m.membershipNumber()));
    }
}
