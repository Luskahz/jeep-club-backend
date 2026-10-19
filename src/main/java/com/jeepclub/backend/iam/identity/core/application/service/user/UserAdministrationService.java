package com.jeepclub.backend.iam.identity.core.application.service.user;

import com.jeepclub.backend.iam.identity.core.domain.exception.UserAlreadyDisabledException;
import com.jeepclub.backend.iam.identity.core.domain.exception.UserNotDisabledException;
import com.jeepclub.backend.iam.identity.api.module.UserAdministration;
import com.jeepclub.backend.iam.identity.api.module.UserDetails;
import com.jeepclub.backend.iam.identity.api.module.exception.RootUserCannotBeDisabledException;
import com.jeepclub.backend.iam.identity.api.module.exception.UserNotFoundException;
import com.jeepclub.backend.iam.identity.core.domain.model.User;
import com.jeepclub.backend.iam.identity.api.module.spi.UserAuthenticationAdministrationPort;
import com.jeepclub.backend.iam.identity.core.port.UserAuthorizationProtectionPort;
import com.jeepclub.backend.iam.identity.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
class UserAdministrationService implements UserAdministration {

    private final UserRepository userRepository;
    private final UserAuthenticationAdministrationPort authenticationAdministrationPort;
    private final UserAuthorizationProtectionPort userAuthorizationProtectionPort;
    private final com.jeepclub.backend.platform.logging.SystemLogService audit;
    private final java.util.List<com.jeepclub.backend.iam.identity.api.module.spi.UserContactChangePort> activationLinks;
    private final java.util.List<com.jeepclub.backend.iam.identity.api.module.spi.UserDeletionGuard> deletionGuards;
    private final java.util.List<com.jeepclub.backend.iam.identity.api.module.spi.UserDeletionCleanup> deletionCleanups;

    @Override
    @Transactional
    public void delete(Long userId, Long actorId, Instant now) {
        java.util.Objects.requireNonNull(actorId, "Lifecycle actor is required");
        findForUpdate(userId);
        if (userAuthorizationProtectionPort.hasRootRole(userId)) {
            throw new com.jeepclub.backend.iam.identity.core.domain.exception.UserLifecycleConflictException("ROOT user is protected from deletion.");
        }
        if (deletionGuards.stream().anyMatch(guard -> guard.hasRetainedReferences(userId))) {
            throw new com.jeepclub.backend.iam.identity.core.domain.exception.UserLifecycleConflictException("User has retained membership, financial or operational relationships.");
        }
        authenticationAdministrationPort.deletePendingAuthentication(userId, now);
        activationLinks.forEach(port -> port.invalidatePendingLinks(userId, now));
        deletionCleanups.forEach(cleanup -> cleanup.removePendingAssociations(userId, actorId, now));
        userRepository.deleteWithHistory(userId, actorId, now);
        audit.recordInTransaction(new com.jeepclub.backend.platform.logging.SystemLogEvent(actorId,
                "IDENTITY_USER_DELETE", "DELETE", "/identity/admin/users/" + userId, 204,
                com.jeepclub.backend.platform.logging.SystemLogOutcome.SUCCESS, 0, null, now));
    }


    @Override
    @Transactional
    public UserDetails disable(Long userId, Instant now) {
        User user = findForUpdate(userId);
        if (userAuthorizationProtectionPort.hasRootRole(userId)) {
            throw new RootUserCannotBeDisabledException(userId);
        }
        try {
            user.disable(now);
        } catch (UserAlreadyDisabledException exception) {
            throw new com.jeepclub.backend.iam.identity.api.module.exception.UserAlreadyDisabledException(userId, exception);
        }

        User saved = userRepository.save(user);
        authenticationAdministrationPort.disableAuthentication(userId, now);
        activationLinks.forEach(port -> port.invalidatePendingLinks(userId, now));
        return UserQueryService.toDetails(saved);
    }

    @Override
    @Transactional
    public UserDetails enable(Long userId, Instant now) {
        User user = findForUpdate(userId);
        try {
            user.enable(now);
        } catch (UserNotDisabledException exception) {
            throw new com.jeepclub.backend.iam.identity.api.module.exception.UserNotDisabledException(userId, exception);
        }

        User saved = userRepository.save(user);
        authenticationAdministrationPort.enableAuthentication(userId, now);
        return UserQueryService.toDetails(saved);
    }

    private User findForUpdate(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    @Transactional
    public UserDetails archive(Long userId, Long actorId, Instant now) {
        java.util.Objects.requireNonNull(actorId, "Lifecycle actor is required");
        User user = findForUpdate(userId);
        if (userAuthorizationProtectionPort.hasRootRole(userId)) {
            throw new com.jeepclub.backend.iam.identity.core.domain.exception.UserLifecycleConflictException(
                    "ROOT user is protected from archival.");
        }
        var previous = user.getStatus();
        if (user.archive(now)) {
            user = userRepository.saveAndFlush(user);
            authenticationAdministrationPort.disableAuthentication(userId, now);
            activationLinks.forEach(port -> port.invalidatePendingLinks(userId, now));
            record(actorId, userId, "ARCHIVE:" + previous + ":ARCHIVED", now);
        }
        return UserQueryService.toDetails(user);
    }

    @Override
    @Transactional
    public UserDetails unarchive(Long userId, Long actorId, Instant now) {
        java.util.Objects.requireNonNull(actorId, "Lifecycle actor is required");
        User user = findForUpdate(userId);
        if (userAuthorizationProtectionPort.hasRootRole(userId)) {
            throw new com.jeepclub.backend.iam.identity.core.domain.exception.UserLifecycleConflictException(
                    "ROOT user is protected from archival transitions.");
        }
        if (user.unarchive(now)) {
            user = userRepository.saveAndFlush(user);
            // Authentication stays DISABLED: only the separate enable command can enable access.
            record(actorId, userId, "UNARCHIVE:ARCHIVED:INACTIVE", now);
        }
        return UserQueryService.toDetails(user);
    }

    private void record(Long actorId, Long target, String operation, Instant now) {
        audit.recordInTransaction(new com.jeepclub.backend.platform.logging.SystemLogEvent(actorId,
                "IDENTITY_USER_" + operation, "PATCH", "/identity/admin/users/" + target, 200,
                com.jeepclub.backend.platform.logging.SystemLogOutcome.SUCCESS, 0, null, now));
    }
}
