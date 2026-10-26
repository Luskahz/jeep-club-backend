package com.jeepclub.backend.memberships.infra.persistence.adapter;

import com.jeepclub.backend.memberships.core.repository.MemberRepository;
import com.jeepclub.backend.memberships.core.domain.model.*;
import com.jeepclub.backend.memberships.core.domain.exception.MemberRegistryException;
import com.jeepclub.backend.memberships.infra.persistence.entity.*;
import com.jeepclub.backend.memberships.infra.persistence.jpa.*;
import com.jeepclub.backend.memberships.infra.persistence.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import java.time.Instant;
import java.util.*;

@Repository @RequiredArgsConstructor
public class MemberRepositoryAdapter implements MemberRepository {
    private final MemberJpaRepository members;
    private final jakarta.persistence.EntityManager entityManager;
    private final MembershipNumberClaimJpaRepository claims;
    private final MemberRegistryAuditJpaRepository audits;
    private final MemberRegistrySequenceJpaRepository sequences;
    public MemberRegistrySequence lockRegistry() {
        var s=sequences.lockCurrent().orElseThrow(() -> new MemberRegistryException(MemberRegistryException.Kind.CONFLICT));
        entityManager.refresh(s,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        return new MemberRegistrySequence(s.getNextValue(),s.getFormatSignature());
    }
    public Optional<String> greatestCurrentNumberForUpdate() { return members.greatestCurrentNumberForUpdate(); }
    public void releaseClaim(MembershipNumber number, Long memberId) {
        var claim=claims.lockByKey(number.claimKey()).orElseThrow(() -> new MemberRegistryException(MemberRegistryException.Kind.CONFLICT));
        if (!Objects.equals(claim.getMemberId(),memberId)) throw new MemberRegistryException(MemberRegistryException.Kind.CONFLICT);
        entityManager.remove(claim); entityManager.flush();
    }
    public Optional<Member> byId(Long id) { return members.findById(id).map(MemberMapper::toDomain); }
    public Optional<Member> byIdentity(Long id) { return members.findByIdentityUserId(id).map(MemberMapper::toDomain); }
    public Optional<Member> lockedByIdentity(Long id) { return members.lockByIdentity(id).map(e -> { entityManager.refresh(e,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE); return MemberMapper.toDomain(e); }); }
    public Optional<Member> byNumber(String number) { return members.findByMembershipNumber(number).map(MemberMapper::toDomain); }
    public List<Member> byIdentities(Collection<Long> ids) { return ids.isEmpty() ? List.of() : members.findByIdentityUserIdIn(ids).stream().map(MemberMapper::toDomain).toList(); }
    public Page<Member> list(Pageable page) { return members.findAll(page).map(MemberMapper::toDomain); }
    public Member save(Member m) {
        try { return MemberMapper.toDomain(members.saveAndFlush(MemberMapper.toEntity(m))); }
        catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException conflict) { throw new MemberRegistryException(MemberRegistryException.Kind.CONFLICT); }
    }
    public boolean claimed(String key) { return claims.existsById(key); }
    public boolean claimedForUpdate(String key) { return claims.lockByKey(key).isPresent(); }
    public void claim(MembershipNumber n, Long memberId, Long actor, Instant now, String reason) {
        if (claimedForUpdate(n.claimKey())) throw new MemberRegistryException(MemberRegistryException.Kind.CONFLICT);
        var e=new MembershipNumberClaimEntity(); e.setClaimKey(n.claimKey()); e.setMembershipNumber(n.value());
        e.setMemberId(memberId); e.setActorUserId(actor); e.setClaimedAt(now); e.setReason(reason);
        try { entityManager.persist(e); entityManager.flush(); }
        catch (DataIntegrityViolationException | org.hibernate.exception.ConstraintViolationException | jakarta.persistence.EntityExistsException conflict) { throw new MemberRegistryException(MemberRegistryException.Kind.CONFLICT); }
    }
    public void audit(MemberRegistryAudit a) {
        var e=new MemberRegistryAuditEntity(); e.setMemberId(a.memberId()); e.setPreviousNumber(a.previousNumber());
        e.setNewNumber(a.newNumber()); e.setActorUserId(a.actorUserId()); e.setOccurredAt(a.occurredAt());
        e.setReason(a.reason()); e.setOperation(a.operation()); e.setOutcome(a.outcome()); audits.saveAndFlush(e);
    }
    public Page<MemberRegistryAudit> history(Long id, Pageable page) { return audits.findByMemberIdOrderByIdDesc(id,page).map(MemberMapper::audit); }
}
