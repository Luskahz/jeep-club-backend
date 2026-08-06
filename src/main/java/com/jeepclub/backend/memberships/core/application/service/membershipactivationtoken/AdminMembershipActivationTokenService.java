package com.jeepclub.backend.memberships.core.application.service.membershipactivationtoken;

import com.jeepclub.backend.memberships.core.application.exception.MembershipApplicationAlreadyProcessedException;
import com.jeepclub.backend.memberships.core.application.exception.MembershipApplicationNotFoundException;
import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;
import com.jeepclub.backend.memberships.core.port.CreateUserWithPendingFirstAccessPort;
import com.jeepclub.backend.memberships.core.port.PendingFirstAccessLink;
import com.jeepclub.backend.memberships.core.repository.MembershipApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AdminMembershipActivationTokenService {

    private final MembershipApplicationRepository membershipApplicationRepository;
    private final CreateUserWithPendingFirstAccessPort createUserPort;
    private final Clock clock;

    @Transactional
    public PendingFirstAccessLink approveWithAccessLink(
            Long applicationId,
            Long reviewedByUserId
    ) {
        Instant now = Instant.now(clock);

        MembershipApplication application = membershipApplicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new MembershipApplicationNotFoundException(applicationId));

        if (application.getStatus() != MembershipApplicationStatus.PENDING) {
            throw new MembershipApplicationAlreadyProcessedException(
                    applicationId,
                    application.getStatus().name()
            );
        }

        PendingFirstAccessLink pendingUser = createUserPort.createPendingUserWithAccessLink(
                application.getName(),
                application.getEmail(),
                application.getCpf(),
                application.getPhoneNumber()
        );

        application.approve(reviewedByUserId, pendingUser.userId(), now);
        membershipApplicationRepository.save(application);

        return pendingUser;
    }
}
