package com.jeepclub.backend.memberships.core.application.query;

import com.jeepclub.backend.memberships.api.module.MembershipIdentityOwnership;
import com.jeepclub.backend.memberships.core.repository.MembershipApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MembershipIdentityOwnershipQuery implements MembershipIdentityOwnership {
    private final MembershipApplicationRepository applications;

    @Override
    @Transactional(readOnly = true)
    public boolean ownsIdentity(Long identityId) {
        return applications.existsByCreatedUserId(identityId);
    }
}
