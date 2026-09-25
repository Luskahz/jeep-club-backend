package com.jeepclub.backend.memberships.infra.persistence.adapter;

import com.jeepclub.backend.memberships.core.domain.model.MembershipBillingConfiguration;
import com.jeepclub.backend.memberships.core.repository.MembershipBillingConfigurationRepository;
import com.jeepclub.backend.memberships.infra.persistence.jpa.MembershipBillingConfigurationJpaRepository;
import com.jeepclub.backend.memberships.infra.persistence.mapper.MembershipBillingConfigurationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MembershipBillingConfigurationRepositoryAdapter
        implements MembershipBillingConfigurationRepository {

    private static final String CURRENT_KEY = "CURRENT";

    private final MembershipBillingConfigurationJpaRepository jpaRepository;

    @Override
    public Optional<MembershipBillingConfiguration> findCurrent() {
        return jpaRepository.findBySingletonKey(CURRENT_KEY)
                .map(MembershipBillingConfigurationMapper::toDomain);
    }

    @Override
    public MembershipBillingConfiguration save(MembershipBillingConfiguration configuration) {
        return MembershipBillingConfigurationMapper.toDomain(
                jpaRepository.save(MembershipBillingConfigurationMapper.toEntity(configuration))
        );
    }
}
