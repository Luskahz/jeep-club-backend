package com.jeepclub.backend.memberships.core.repository;
import com.jeepclub.backend.memberships.core.domain.model.*;
import org.springframework.data.domain.*;
import java.time.Instant;
import java.util.*;
public interface MemberRepository {
    MemberRegistrySequence lockRegistry();
    void advanceSequence(long next, String signature);
    Optional<Member> byId(Long id);
    Optional<Member> byIdentity(Long identityId);
    Optional<Member> lockedByIdentity(Long identityId);
    Optional<Member> byNumber(String number);
    List<Member> byIdentities(Collection<Long> ids);
    Page<Member> list(Pageable page);
    Member save(Member member);
    boolean claimed(String claimKey);
    boolean claimedForUpdate(String claimKey);
    void claim(MembershipNumber number, Long memberId, Long actor, Instant now, String reason);
    void audit(MemberRegistryAudit audit);
    Page<MemberRegistryAudit> history(Long memberId, Pageable page);
}
