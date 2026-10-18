package com.jeepclub.backend.memberships.core.application.service.member;

import com.jeepclub.backend.memberships.core.domain.exception.MemberRegistryException;
import com.jeepclub.backend.memberships.core.domain.model.*;
import com.jeepclub.backend.memberships.core.port.*;
import com.jeepclub.backend.memberships.core.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class AdminMemberService {
    private final MemberRepository members;
    private final MemberIdentityPort identities;
    private final MemberIssuancePolicy policy;
    private final Clock clock;

    @Transactional public Member assign(Long userId, String number, Long actor, String reason) {
        validateActorReason(actor,reason); var n=new MembershipNumber(number);
        boolean active=identities.lockAndIsActive(userId);
        members.lockRegistry();
        var existing=members.lockedByIdentity(userId);
        if(existing.isPresent()) {
            if(existing.get().membershipNumber().equals(number)) return existing.get();
            throw conflict();
        }
        if(!active || members.claimedForUpdate(n.claimKey())) throw conflict();
        return create(userId,n,actor,reason,"ASSIGN");
    }
    @Transactional public Member correct(Long userId, String number, long expectedVersion, Long actor, String reason) {
        validateActorReason(actor,reason); var n=new MembershipNumber(number);
        identities.lockAndIsActive(userId); members.lockRegistry();
        var old=members.lockedByIdentity(userId).orElseThrow(AdminMemberService::notFound);
        if(old.version()==null || old.version()!=expectedVersion) throw conflict();
        if(old.membershipNumber().equals(number)) return old;
        if(members.claimedForUpdate(n.claimKey())) throw conflict();
        Instant now=Instant.now(clock);
        members.claim(n,old.id(),actor,now,reason);
        var updated=members.save(old.correct(number,expectedVersion,now));
        members.audit(new MemberRegistryAudit(null,updated.id(),old.membershipNumber(),number,actor,now,reason,"CORRECT","SUCCESS"));
        return updated;
    }
    @Transactional public void reserve(String number, Long actor, String reason) {
        validateActorReason(actor,reason); var n=new MembershipNumber(number); members.lockRegistry();
        if(members.claimedForUpdate(n.claimKey())) throw conflict();
        var now=Instant.now(clock); members.claim(n,null,actor,now,reason);
        members.audit(new MemberRegistryAudit(null,null,null,number,actor,now,reason,"RESERVE","SUCCESS"));
    }
    @Transactional public Optional<Member> issueOnCompletion(Long userId) {
        return policy.enabled() ? Optional.of(generate(userId,userId,"ONBOARDING_COMPLETED")) : Optional.empty();
    }
    @Transactional public Member generate(Long userId, Long actor, String reason) {
        validateActorReason(actor,reason);
        if(!policy.enabled()) throw new MemberRegistryException(MemberRegistryException.Kind.DISABLED);
        boolean active=identities.lockAndIsActive(userId); var sequence=members.lockRegistry();
        var existing=members.lockedByIdentity(userId); if(existing.isPresent()) return existing.get();
        if(!active) throw conflict();
        if(sequence.formatSignature()!=null && !sequence.formatSignature().equals(policy.signature())) throw conflict();
        long next=sequence.nextValue()==null ? policy.initialNumber() : sequence.nextValue();
        // Bounded search prevents an exhausted/reserved range from monopolizing the global lock.
        for(int i=0;i<10000;i++) {
            if(next<=0 || next==Long.MAX_VALUE) throw conflict();
            var number=new MembershipNumber(policy.format(next++));
            if(!members.claimedForUpdate(number.claimKey())) {
                members.advanceSequence(next,policy.signature());
                return create(userId,number,actor,reason,"GENERATE");
            }
        }
        throw conflict();
    }
    private Member create(Long userId, MembershipNumber number, Long actor, String reason, String operation) {
        Instant now=Instant.now(clock);
        var member=members.save(Member.create(userId,number.value(),now));
        members.claim(number,member.id(),actor,now,reason);
        members.audit(new MemberRegistryAudit(null,member.id(),null,number.value(),actor,now,reason,operation,"SUCCESS"));
        return member;
    }
    @Transactional(readOnly=true) public Member byIdentity(Long id) { return members.byIdentity(id).orElseThrow(AdminMemberService::notFound); }
    @Transactional(readOnly=true) public Member byNumber(String number) { new MembershipNumber(number); return members.byNumber(number).orElseThrow(AdminMemberService::notFound); }
    @Transactional(readOnly=true) public Page<Member> list(Pageable page) { return members.list(page); }
    @Transactional(readOnly=true) public Page<MemberRegistryAudit> history(Long userId, Pageable page) { return members.history(byIdentity(userId).id(),page); }
    public static void validateActorReason(Long actor, String reason) {
        if(actor==null || actor<=0 || reason==null || reason.isBlank() || reason.length()>500
                || reason.chars().anyMatch(Character::isISOControl)) throw new MemberRegistryException(MemberRegistryException.Kind.INVALID);
    }
    private static MemberRegistryException conflict() { return new MemberRegistryException(MemberRegistryException.Kind.CONFLICT); }
    private static MemberRegistryException notFound() { return new MemberRegistryException(MemberRegistryException.Kind.NOT_FOUND); }
}
