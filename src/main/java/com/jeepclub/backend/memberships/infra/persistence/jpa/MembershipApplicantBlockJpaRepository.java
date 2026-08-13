package com.jeepclub.backend.memberships.infra.persistence.jpa;

import com.jeepclub.backend.memberships.infra.persistence.entity.MembershipApplicantBlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MembershipApplicantBlockJpaRepository
        extends JpaRepository<MembershipApplicantBlockEntity, Long> {

    boolean existsByCpfAndUnblockedAtIsNull(String cpf);

    Optional<MembershipApplicantBlockEntity> findByCpfAndUnblockedAtIsNull(String cpf);
}
