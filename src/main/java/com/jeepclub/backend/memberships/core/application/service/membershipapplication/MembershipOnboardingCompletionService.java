package com.jeepclub.backend.memberships.core.application.service.membershipapplication;

import com.jeepclub.backend.memberships.api.module.MembershipOnboardingCompletion;
import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.memberships.core.repository.MembershipApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MembershipOnboardingCompletionService implements MembershipOnboardingCompletion {

    private final MembershipApplicationRepository repository;

    @Override
    @Transactional
    public void completeApprovedApplicationForIdentity(Long identityId, Instant now) {
        Objects.requireNonNull(identityId, "identityId cannot be null");
        Objects.requireNonNull(now, "now cannot be null");
        repository.findByCreatedUserIdAndStatus(identityId, MembershipApplicationStatus.APPROVED)
                .ifPresent(application -> {
                    application.complete(now);
                    repository.save(application);
                });
    }
}
