package com.jeepclub.backend.membership.infra.persistence.jpa;

import com.jeepclub.backend.membership.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.membership.infra.persistence.entity.MembershipApplicationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipApplicationJpaRepository
        extends JpaRepository<MembershipApplicationEntity, Long> {

    Optional<MembershipApplicationEntity> findByCpf(String cpf);

    List<MembershipApplicationEntity> findAllByStatus(MembershipApplicationStatus status);

    Page<MembershipApplicationEntity> findAll(Pageable pageable);

    Page<MembershipApplicationEntity> findAllByStatus(MembershipApplicationStatus status, Pageable pageable);

    boolean existsByCpf(String cpf);
}