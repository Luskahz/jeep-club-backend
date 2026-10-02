package com.jeepclub.backend.memberships.core.application.service.membershipbilling;

import com.jeepclub.backend.billing.api.module.ChargeDefinitionQuery;
import com.jeepclub.backend.memberships.core.application.exception.InvalidMembershipChargeDefinitionException;
import com.jeepclub.backend.memberships.core.application.exception.MembershipBillingConfigurationNotFoundException;
import com.jeepclub.backend.memberships.core.domain.model.MembershipBillingConfiguration;
import com.jeepclub.backend.memberships.core.repository.MembershipBillingConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminMembershipBillingConfigurationService {

    private final MembershipBillingConfigurationRepository repository;
    private final ChargeDefinitionQuery chargeDefinitionQuery;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Optional<MembershipBillingConfiguration> getCurrent() {
        return repository.findCurrent();
    }

    @Transactional
    public MembershipBillingConfiguration configure(
            Long chargeDefinitionId,
            boolean enforcementEnabled
    ) {
        validateChargeDefinition(chargeDefinitionId);
        Instant now = Instant.now(clock);

        MembershipBillingConfiguration configuration = repository.findCurrent()
                .map(current -> {
                    current.replaceChargeDefinition(chargeDefinitionId, now);
                    current.setEnforcementEnabled(enforcementEnabled, now);
                    return current;
                })
                .orElseGet(() -> MembershipBillingConfiguration.create(
                        chargeDefinitionId,
                        enforcementEnabled,
                        now
                ));

        return repository.save(configuration);
    }

    @Transactional
    public MembershipBillingConfiguration setEnforcementEnabled(boolean enabled) {
        MembershipBillingConfiguration configuration = repository.findCurrent()
                .orElseThrow(MembershipBillingConfigurationNotFoundException::new);
        if (enabled && !configuration.isEnforcementEnabled()) {
            validateChargeDefinition(configuration.getChargeDefinitionId());
        }
        configuration.setEnforcementEnabled(enabled, Instant.now(clock));
        return repository.save(configuration);
    }

    private void validateChargeDefinition(Long chargeDefinitionId) {
        if (!chargeDefinitionQuery.isActive(chargeDefinitionId)) {
            throw new InvalidMembershipChargeDefinitionException();
        }
    }
}
