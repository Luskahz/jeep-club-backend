package com.jeepclub.backend.memberships.core.application.service.membershipapplication;

import com.jeepclub.backend.memberships.core.application.exception.*;
import com.jeepclub.backend.memberships.core.application.service.memberactivationtoken.MemberActivationTokenService;
import com.jeepclub.backend.memberships.core.application.service.membershipapplicantblock.AdminMembershipApplicantBlockService;
import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;
import com.jeepclub.backend.memberships.core.port.*;
import com.jeepclub.backend.memberships.core.repository.MemberActivationTokenRepository;
import com.jeepclub.backend.memberships.core.repository.MembershipApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminMembershipApplicationService {
    private final MembershipApplicationRepository membershipApplicationRepository;
    private final AdminMembershipApplicantBlockService adminMembershipApplicantBlockService;
    private final CreateUserWithPendingFirstAccessPort createUserPort;
    private final MembershipRejectionNotifier rejectionNotifier;
    private final MemberActivationTokenService activationTokenService;
    private final Clock clock;
    private final PendingMembershipIdentityReversal identityReversal;
    private final MemberActivationTokenRepository activationTokens;
    private final MembershipReviewHistory reviewHistory;
    private final PendingMembershipAuthentication pendingAuthentication;

    @Transactional(readOnly = true)
    public Page<MembershipApplication> listAll(Pageable pageable) { return membershipApplicationRepository.findAll(pageable); }
    @Transactional(readOnly = true)
    public Page<MembershipApplication> listByStatus(MembershipApplicationStatus status, Pageable pageable) {
        return membershipApplicationRepository.findAllByStatus(status, pageable);
    }
    @Transactional(readOnly = true)
    public MembershipApplication findById(Long id) {
        return membershipApplicationRepository.findById(id).orElseThrow(() -> new MembershipApplicationNotFoundException(id));
    }

    @Transactional
    public PendingFirstAccessUser approveWithTemporaryPassword(Long applicationId, Long actorId) {
        MembershipApplication application = findPendingApplication(applicationId);
        PendingFirstAccessUser user = createUserPort.createPendingUserWithTemporaryPassword(
                application.getName(), application.getEmail(), application.getCpf(), application.getPhoneNumber());
        approve(application, actorId, user.userId(), "APPROVE_TEMPORARY_PASSWORD");
        return user;
    }

    @Transactional
    public AccessLinkApproval approveWithAccessLink(Long applicationId, Long actorId) {
        MembershipApplication application = findPendingApplication(applicationId);
        if (application.getEmail() == null || application.getEmail().isBlank()) throw new MembershipEmailRequiredException();
        PendingFirstAccessIdentity user = createUserPort.createPendingUserForActivationLink(
                application.getName(), application.getEmail(), application.getCpf(), application.getPhoneNumber());
        application = approve(application, actorId, user.userId(), "APPROVE_ACCESS_LINK");
        return new AccessLinkApproval(user.userId(), activationTokenService.issueAndSend(application));
    }

    @Transactional
    public void resendActivationLink(Long applicationId) {
        MembershipApplication application = lockProvisionedApplication(applicationId);
        requireStatus(application, MembershipApplicationStatus.APPROVED);
        activationTokenService.issueAndSend(application);
    }

    @Transactional
    public void reject(Long applicationId, Long actorId, String reason) { reject(applicationId, actorId, reason, false); }
    @Transactional
    public void rejectAndBlock(Long applicationId, Long actorId, String reason) { reject(applicationId, actorId, reason, true); }

    private void reject(Long id, Long actorId, String reason, boolean block) {
        MembershipApplication application = lockProvisionedApplication(id);
        boolean reverse = application.getStatus() == MembershipApplicationStatus.APPROVED;
        if (!reverse) requireStatus(application, MembershipApplicationStatus.PENDING);
        if (reverse) pendingAuthentication.requirePending(application.getCreatedUserId());
        Instant now = Instant.now(clock);
        reviewHistory.record(application, reverse ? (block ? "REVERSE_AND_BLOCK" : "REVERSE_APPROVAL")
                : (block ? "REJECT_AND_BLOCK" : "REJECT"), "REJECTED", application.getCreatedUserId(), actorId, now);
        if (reverse) {
            activationTokens.invalidateAllByApplicationId(id, now);
            application.rejectApproved(actorId, reason, now);
        } else application.reject(actorId, reason, now);
        // The retained-reference guard must observe REJECTED. Failure below rolls this flush back.
        membershipApplicationRepository.saveAndFlush(application);
        if (reverse) identityReversal.delete(application.getCreatedUserId(), actorId, now);
        if (block) adminMembershipApplicantBlockService.block(application.getCpf(), reason, actorId, now);
        if (application.getEmail() != null && !application.getEmail().isBlank() && reason != null && !reason.isBlank()) {
            rejectionNotifier.afterCommit(application.getEmail(), application.getName(), reason.trim());
        }
    }

    private MembershipApplication approve(MembershipApplication application, Long actorId, Long userId, String method) {
        Instant now = Instant.now(clock);
        reviewHistory.record(application, method, "APPROVED", userId, actorId, now);
        application.approve(actorId, userId, now);
        return membershipApplicationRepository.saveAndFlush(application);
    }

    private MembershipApplication findPendingApplication(Long id) {
        MembershipApplication application = membershipApplicationRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new MembershipApplicationNotFoundException(id));
        requireStatus(application, MembershipApplicationStatus.PENDING);
        return application;
    }

    private MembershipApplication lockProvisionedApplication(Long id) {
        MembershipApplication candidate = findById(id);
        boolean provisioned = candidate.getStatus() == MembershipApplicationStatus.APPROVED;
        if (provisioned) {
            if (candidate.getCreatedUserId() == null) throw new MembershipFirstAccessConflictException("Approved application has no provisioned Identity.");
            identityReversal.lock(candidate.getCreatedUserId());
        }
        MembershipApplication current = membershipApplicationRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new MembershipApplicationNotFoundException(id));
        // Never invert User -> application order if approval committed while this request waited.
        if ((!provisioned && current.getStatus() == MembershipApplicationStatus.APPROVED)
                || (provisioned && !Objects.equals(candidate.getCreatedUserId(), current.getCreatedUserId()))) {
            throw new MembershipFirstAccessConflictException("Membership provisioning changed concurrently; retry the request.");
        }
        return current;
    }

    private void requireStatus(MembershipApplication application, MembershipApplicationStatus expected) {
        if (application.getStatus() != expected) throw new MembershipApplicationAlreadyProcessedException(
                application.getId(), application.getStatus().name());
    }
}
