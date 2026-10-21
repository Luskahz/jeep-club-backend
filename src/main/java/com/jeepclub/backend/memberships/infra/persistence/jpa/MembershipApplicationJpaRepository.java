package com.jeepclub.backend.memberships.infra.persistence.jpa;

import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.memberships.infra.persistence.entity.MembershipApplicationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipApplicationJpaRepository
        extends JpaRepository<MembershipApplicationEntity, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from MembershipApplicationEntity a where a.id = :id")
    Optional<MembershipApplicationEntity> findByIdForUpdate(Long id);

    @org.springframework.data.jpa.repository.Query("select a.id from MembershipApplicationEntity a where a.createdUserId = :userId and a.status = :status")
    Optional<Long> findIdByCreatedUserIdAndStatus(Long userId, MembershipApplicationStatus status);

    boolean existsByCreatedUserId(Long createdUserId);

    Optional<MembershipApplicationEntity> findByCpf(String cpf);

    List<MembershipApplicationEntity> findAllByStatus(MembershipApplicationStatus status);

    Page<MembershipApplicationEntity> findAll(Pageable pageable);

    Page<MembershipApplicationEntity> findAllByStatus(MembershipApplicationStatus status, Pageable pageable);

    boolean existsByCpf(String cpf);
    boolean existsByEmail(String email);
    Optional<MembershipApplicationEntity> findByCpfAndStatus(
            String cpf,
            MembershipApplicationStatus status
    );

    boolean existsByEmailAndStatus(
            String email,
            MembershipApplicationStatus status
    );

    Optional<MembershipApplicationEntity> findByCreatedUserIdAndStatus(
            Long createdUserId,
            MembershipApplicationStatus status
    );
}
