package com.jeepclub.backend.memberships.core.application.service.membershipapplication;

import com.jeepclub.backend.memberships.core.application.exception.MembershipApplicationAlreadyProcessedException;
import com.jeepclub.backend.memberships.core.application.exception.MembershipApplicationNotFoundException;
import com.jeepclub.backend.memberships.core.application.service.membershipapplicantblock.AdminMembershipApplicantBlockService;
import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;
import com.jeepclub.backend.memberships.core.port.CreateUserWithPendingFirstAccessPort;
import com.jeepclub.backend.memberships.core.port.MemberActivationMailSender;
import com.jeepclub.backend.memberships.core.port.PendingFirstAccessLink;
import com.jeepclub.backend.memberships.core.port.PendingFirstAccessUser;
import com.jeepclub.backend.memberships.core.repository.MembershipApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminMembershipApplicationService {

    private final MembershipApplicationRepository membershipApplicationRepository;
    private final AdminMembershipApplicantBlockService adminMembershipApplicantBlockService;
    private final CreateUserWithPendingFirstAccessPort createUserPort;
    private final MemberActivationMailSender mailSender;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<MembershipApplication> listAll(Pageable pageable) {
        return membershipApplicationRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<MembershipApplication> listByStatus(
            MembershipApplicationStatus status,
            Pageable pageable
    ) {
        return membershipApplicationRepository.findAllByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<MembershipApplication> findById(Long id) {
        return membershipApplicationRepository.findById(id);
    }

    @Transactional
    public PendingFirstAccessUser approveWithTemporaryPassword(
            Long applicationId,
            Long reviewedByUserId
    ) {
        Instant now = Instant.now(clock);
        MembershipApplication application = findPendingApplication(applicationId);

        PendingFirstAccessUser pendingUser = createUserPort.createPendingUserWithTemporaryPassword(
                application.getName(),
                application.getEmail(),
                application.getCpf(),
                application.getPhoneNumber()
        );

        approve(application, reviewedByUserId, pendingUser.userId(), now);
        return pendingUser;
    }

    @Transactional
    public PendingFirstAccessLink approveWithAccessLink(
            Long applicationId,
            Long reviewedByUserId
    ) {
        Instant now = Instant.now(clock);
        MembershipApplication application = findPendingApplication(applicationId);

        PendingFirstAccessLink pendingUser = createUserPort.createPendingUserWithAccessLink(
                application.getName(),
                application.getEmail(),
                application.getCpf(),
                application.getPhoneNumber()
        );

        approve(application, reviewedByUserId, pendingUser.userId(), now);
        return pendingUser;
    }

    @Transactional
    public void reject(Long applicationId, Long reviewedByUserId, String reason) {
        Instant now = Instant.now(clock);
        rejectApplication(applicationId, reviewedByUserId, reason, now);
    }

    @Transactional
    public void rejectAndBlock(Long applicationId, Long reviewedByUserId, String reason) {
        Instant now = Instant.now(clock);
        MembershipApplication application = findPendingApplication(applicationId);

        adminMembershipApplicantBlockService.block(
                application.getCpf(),
                reason,
                reviewedByUserId,
                now
        );
        rejectApplication(application, reviewedByUserId, reason, now);
    }

    private void approve(
            MembershipApplication application,
            Long reviewedByUserId,
            Long createdUserId,
            Instant now
    ) {
        application.approve(reviewedByUserId, createdUserId, now);
        membershipApplicationRepository.save(application);
    }

    private MembershipApplication rejectApplication(
            Long applicationId,
            Long reviewedByUserId,
            String reason,
            Instant now
    ) {
        MembershipApplication application = findPendingApplication(applicationId);
        rejectApplication(application, reviewedByUserId, reason, now);
        return application;
    }

    private void rejectApplication(
            MembershipApplication application,
            Long reviewedByUserId,
            String reason,
            Instant now
    ) {
        application.reject(reviewedByUserId, reason, now);
        membershipApplicationRepository.save(application);

        if (reason != null && !reason.isBlank()) {
            mailSender.sendRejectionNotice(
                    application.getEmail(),
                    application.getName(),
                    reason.trim()
            );
        }
    }

    private MembershipApplication findPendingApplication(Long applicationId) {
        MembershipApplication application = membershipApplicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new MembershipApplicationNotFoundException(applicationId));

        if (application.getStatus() != MembershipApplicationStatus.PENDING) {
            throw new MembershipApplicationAlreadyProcessedException(
                    applicationId,
                    application.getStatus().name()
            );
        }

        return application;
    }
}
