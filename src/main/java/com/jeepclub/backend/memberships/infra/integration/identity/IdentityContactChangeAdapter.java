package com.jeepclub.backend.memberships.infra.integration.identity;

import com.jeepclub.backend.iam.identity.api.module.spi.UserContactChangePort;
import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.memberships.core.repository.MembershipApplicationRepository;
import com.jeepclub.backend.memberships.core.repository.MemberActivationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class IdentityContactChangeAdapter implements UserContactChangePort {
    private final MembershipApplicationRepository applications;
    private final MemberActivationTokenRepository tokens;
    @Override
    public void invalidatePendingLinks(Long userId, Instant now) {
        applications.findByCreatedUserIdAndStatus(userId, MembershipApplicationStatus.APPROVED)
                .ifPresent(application -> tokens.invalidateAllByApplicationId(application.getId(), now));
    }
}
