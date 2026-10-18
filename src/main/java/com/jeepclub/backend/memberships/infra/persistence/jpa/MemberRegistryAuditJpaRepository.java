package com.jeepclub.backend.memberships.infra.persistence.jpa;
import com.jeepclub.backend.memberships.infra.persistence.entity.MemberRegistryAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
public interface MemberRegistryAuditJpaRepository extends JpaRepository<MemberRegistryAuditEntity,Long> {
    Page<MemberRegistryAuditEntity> findByMemberIdOrderByIdDesc(Long memberId, Pageable page);
}
