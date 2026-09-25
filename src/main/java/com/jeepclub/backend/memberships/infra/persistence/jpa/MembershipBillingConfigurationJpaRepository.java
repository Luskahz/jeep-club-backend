package com.jeepclub.backend.memberships.infra.persistence.jpa;

import com.jeepclub.backend.memberships.infra.persistence.entity.MembershipBillingConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MembershipBillingConfigurationJpaRepository
        extends JpaRepository<MembershipBillingConfigurationEntity, Long> {

    Optional<MembershipBillingConfigurationEntity> findBySingletonKey(String singletonKey);
}
