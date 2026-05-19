package com.jeepclub.backend.membership.core.application.service;

import com.jeepclub.backend.authentication.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.authentication.core.domain.model.MembershipApplication;
import com.jeepclub.backend.authentication.core.repository.MembershipApplicationRepository;
import com.jeepclub.backend.membership.core.application.exception.MembershipApplicationAlreadyProcessedException;
import com.jeepclub.backend.membership.core.application.exception.MembershipApplicationNotFoundException;
import com.jeepclub.backend.membership.core.port.MemberActivationMailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RejectMembershipApplicationService {

    private final MembershipApplicationRepository membershipApplicationRepository;
    private final MemberActivationMailSender mailSender;

    @Transactional
    public void reject(Long applicationId, String reason) {
        Instant now = Instant.now();

        MembershipApplication application = membershipApplicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new MembershipApplicationNotFoundException(applicationId));

        if (application.getStatus() != MembershipApplicationStatus.PENDING_ACTIVATION) {
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