package com.jeepclub.backend.membership.core.application.service;

import com.jeepclub.backend.membership.core.application.exception.MembershipApplicationAlreadyProcessedException;
import com.jeepclub.backend.membership.core.application.exception.MembershipApplicationNotFoundException;
import com.jeepclub.backend.membership.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.membership.core.domain.model.MembershipApplication;
import com.jeepclub.backend.membership.core.port.MemberActivationMailSender;
import com.jeepclub.backend.membership.core.repository.MembershipApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RejectMembershipApplicationService {

    private final MembershipApplicationRepository membershipApplicationRepository;
    private final MemberActivationMailSender mailSender;
    private final Clock clock;

    @Transactional
    public void reject(Long applicationId, String reason) {
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

        application.markAsRejected(now);
        membershipApplicationRepository.save(application);

        if (reason != null && !reason.isBlank()) {
            mailSender.sendRejectionNotice(application.getEmail(), application.getName(), reason.trim());
        }
    }
}