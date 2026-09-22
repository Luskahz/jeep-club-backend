package com.jeepclub.backend.memberships.core.application.query;

import com.jeepclub.backend.billing.api.module.MembershipChargeQuery;
import com.jeepclub.backend.billing.api.module.MembershipChargeResult;
import com.jeepclub.backend.memberships.api.module.MembershipAccessQuery;
import com.jeepclub.backend.memberships.api.module.MembershipAccessResult;
import com.jeepclub.backend.memberships.api.module.exception.MembershipAccessUnavailableException;
import com.jeepclub.backend.memberships.core.domain.model.MembershipBillingConfiguration;
import com.jeepclub.backend.memberships.core.repository.MembershipBillingConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MembershipAccessQueryService implements MembershipAccessQuery {

    private static final Logger log = LoggerFactory.getLogger(MembershipAccessQueryService.class);

    private final MembershipBillingConfigurationRepository configurationRepository;
    private final MembershipChargeQuery membershipChargeQuery;

    @Override
    @Transactional(readOnly = true)
    public MembershipAccessResult evaluate(Long userId) {
        validateUserId(userId);

        return configurationRepository.findCurrent()
                .filter(MembershipBillingConfiguration::isEnforcementEnabled)
                .map(configuration -> evaluateConfigured(configuration, userId))
                .orElse(MembershipAccessResult.ALLOWED);
    }

    private MembershipAccessResult evaluateConfigured(
            MembershipBillingConfiguration configuration,
            Long userId
    ) {
        MembershipChargeResult result;
        try {
            result = membershipChargeQuery.evaluate(
                    configuration.getChargeDefinitionId(),
                    userId
            );
        } catch (RuntimeException exception) {
            log.error(
                    "membership_access_unavailable chargeDefinitionId={} userId={} cause={}",
                    configuration.getChargeDefinitionId(),
                    userId,
                    exception.getClass().getSimpleName()
            );
            throw new MembershipAccessUnavailableException(exception);
        }

        return switch (result) {
            case WITHIN_PAYMENT_PERIOD, SATISFIED, CANCELED -> MembershipAccessResult.ALLOWED;
            case PAYMENT_REQUIRED -> MembershipAccessResult.PAYMENT_REQUIRED;
            case CHARGE_NOT_FOUND -> {
                log.warn(
                        "membership_charge_not_found chargeDefinitionId={} userId={}",
                        configuration.getChargeDefinitionId(),
                        userId
                );
                yield MembershipAccessResult.CHARGE_NOT_FOUND;
            }
        };
    }

    private static void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId must be greater than zero");
        }
    }
}
