package com.jeepclub.backend.memberships.core.repository;

import com.jeepclub.backend.memberships.core.domain.model.MembershipBillingConfiguration;

import java.util.Optional;

public interface MembershipBillingConfigurationRepository {

    Optional<MembershipBillingConfiguration> findCurrent();

    MembershipBillingConfiguration save(MembershipBillingConfiguration configuration);
}
