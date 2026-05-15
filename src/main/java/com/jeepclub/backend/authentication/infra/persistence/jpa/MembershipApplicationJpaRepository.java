package com.jeepclub.backend.authentication.infra.persistence.jpa;

import com.jeepclub.backend.authentication.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.authentication.infra.persistence.entities.MembershipApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipApplicationJpaRepository extends JpaRepository<MembershipApplicationEntity, Long> {

    boolean existsByCpfAndStatus(String cpf, MembershipApplicationStatus status);

    boolean existsByEmailAndStatus(String email, MembershipApplicationStatus status);
}