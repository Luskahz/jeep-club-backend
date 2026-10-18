package com.jeepclub.backend.memberships.infra.persistence.mapper;
import com.jeepclub.backend.memberships.core.domain.model.*;
import com.jeepclub.backend.memberships.infra.persistence.entity.*;
public final class MemberMapper {
    private MemberMapper() { }
    public static Member toDomain(MemberEntity e) {
        return new Member(e.getId(),e.getIdentityUserId(),e.getMembershipNumber(),e.getCreatedAt(),e.getUpdatedAt(),e.getVersion());
    }
    public static MemberEntity toEntity(Member m) {
        var e=new MemberEntity(); e.setId(m.id()); e.setIdentityUserId(m.identityUserId());
        e.setMembershipNumber(m.membershipNumber()); e.setCreatedAt(m.createdAt()); e.setUpdatedAt(m.updatedAt()); e.setVersion(m.version());
        return e;
    }
    public static MemberRegistryAudit audit(MemberRegistryAuditEntity e) {
        return new MemberRegistryAudit(e.getId(),e.getMemberId(),e.getPreviousNumber(),e.getNewNumber(),e.getActorUserId(),e.getOccurredAt(),e.getReason(),e.getOperation(),e.getOutcome());
    }
}
