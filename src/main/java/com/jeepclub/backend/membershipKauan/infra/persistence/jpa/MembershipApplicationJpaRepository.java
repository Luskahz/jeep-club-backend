package com.jeepclub.backend.membershipKauan.infra.persistence.jpa;

import com.jeepclub.backend.membershipKauan.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.membershipKauan.infra.persistence.entity.MembershipApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipApplicationJpaRepository extends JpaRepository<MembershipApplicationEntity, Long> {

    boolean existsByCpfAndStatus(String cpf, MembershipApplicationStatus status);

    boolean existsByEmailAndStatus(String email, MembershipApplicationStatus status);
}