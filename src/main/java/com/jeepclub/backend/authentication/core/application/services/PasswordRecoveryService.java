package com.jeepclub.backend.authentication.core.application.services;

import com.jeepclub.backend.authentication.core.application.exceptions.login.PasswordRecoveryRequestNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.tokenhash.TokenInvalidException;
import com.jeepclub.backend.authentication.core.application.exceptions.tokenhash.TokenNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserIdNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserInvalidCredentialsException;
import com.jeepclub.backend.authentication.core.application.results.PasswordResetLinkAdminResult;
import com.jeepclub.backend.authentication.core.application.results.TemporaryPasswordAdminResult;
import com.jeepclub.backend.authentication.core.application.results.admin.recovery.AdminPasswordRecoveryRequestResult;
import com.jeepclub.backend.authentication.core.domain.model.PasswordRecoveryRequest;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.port.ApplicationTimeProperties;
import com.jeepclub.backend.authentication.core.port.ApplicationUrlProperties;
import com.jeepclub.backend.authentication.core.port.NotificationPort;
import com.jeepclub.backend.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.authentication.core.port.RandomPasswordGenerator;
import com.jeepclub.backend.authentication.core.port.RefreshTokenGenerator;
import com.jeepclub.backend.authentication.core.port.RefreshTokenHashService;
import com.jeepclub.backend.authentication.core.repository.PasswordRecoveryRequestRepository;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {

    private final UserRepository userRepository;
    private final PasswordRecoveryRequestRepository passwordRecoveryRequestRepository;
    private final NotificationPort notificationPort;
    private final RandomPasswordGenerator randomPasswordGenerator;
    private final RefreshTokenGenerator tokenGenerator;
    private final RefreshTokenHashService tokenHashService;
    private final PasswordHasher passwordHasher;
    private final ApplicationTimeProperties authTimeProperties;
    private final ApplicationUrlProperties applicationUrlProperties;
    private final Clock clock;

    @Transactional
    public PasswordRecoveryRequest createOrGetOpenRecoveryRequest(String cpf) {
        Instant now = Instant.now(clock);

        User user = userRepository.findByCpf(cpf)
                .orElseThrow(UserInvalidCredentialsException::new);

        user.assertCanRequestPasswordChange();

        return getOrCreateOpenRecoveryRequest(user.getId(), now);
    }

    @Transactional
    public PasswordRecoveryRequest sendRecoveryEmailToken(String cpf) {
        Instant now = Instant.now(clock);

        User user = userRepository.findByCpf(cpf)
                .orElseThrow(UserInvalidCredentialsException::new);

        user.assertCanRequestPasswordChange();

        PasswordRecoveryRequest recoveryRequest =
                getOrCreateOpenRecoveryRequest(user.getId(), now);

        String rawToken = tokenGenerator.generate();
        String hashedToken = tokenHashService.hash(rawToken);

        recoveryRequest.changeToEmailTokenMethod(hashedToken, now);

        PasswordRecoveryRequest savedRecoveryRequest =
                passwordRecoveryRequestRepository.save(recoveryRequest);

        notificationPort.sendPasswordResetLink(
                user.getEmail(),
                buildResetLink(rawToken)
        );

        return savedRecoveryRequest;
    }

    @Transactional
    public void resetPasswordByToken(String rawToken, String newPassword) {
        Instant now = Instant.now(clock);

        String hashedToken = tokenHashService.hash(rawToken);

        PasswordRecoveryRequest recoveryRequest =
                passwordRecoveryRequestRepository.findByTokenHash(hashedToken)
                        .orElseThrow(() -> new TokenNotFoundException("Token invalid or expired."));

        if (!recoveryRequest.isTokenBased()) {
            throw new TokenInvalidException("Token invalid or expired.");
        }

        if (!recoveryRequest.isOpen(now)) {
            throw new TokenInvalidException("Token invalid or expired.");
        }

        User user = userRepository.findById(recoveryRequest.getUserId())
                .orElseThrow(() -> new UserIdNotFoundException("User not found with this id."));

        String newPasswordHash = passwordHasher.hash(newPassword);

        user.changePassword(newPasswordHash, now);
        recoveryRequest.resolve(now);

        userRepository.save(user);
        passwordRecoveryRequestRepository.save(recoveryRequest);
    }

    @Transactional
    public TemporaryPasswordAdminResult generateTemporaryPasswordByAdmin(Long targetUserId) {
        Instant now = Instant.now(clock);

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserIdNotFoundException("User target not found."));

        user.assertCanRequestPasswordChange();

        PasswordRecoveryRequest recoveryRequest =
                getOrCreateOpenRecoveryRequest(user.getId(), now);

        recoveryRequest.changeToAdminTemporaryPasswordMethod(now);

        String temporaryPassword = randomPasswordGenerator.generateSecurePassword();
        String temporaryPasswordHash = passwordHasher.hash(temporaryPassword);

        user.changeToTemporaryPassword(temporaryPasswordHash, now);

        userRepository.save(user);
        PasswordRecoveryRequest savedRecoveryRequest =
                passwordRecoveryRequestRepository.save(recoveryRequest);

        return new TemporaryPasswordAdminResult(
                temporaryPassword,
                savedRecoveryRequest
        );
    }

    @Transactional
    public PasswordResetLinkAdminResult generateResetLinkByAdmin(Long targetUserId) {
        Instant now = Instant.now(clock);

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserIdNotFoundException("User target not found."));

        user.assertCanRequestPasswordChange();

        PasswordRecoveryRequest recoveryRequest =
                getOrCreateOpenRecoveryRequest(user.getId(), now);

        String rawToken = tokenGenerator.generate();
        String hashedToken = tokenHashService.hash(rawToken);

        recoveryRequest.changeToAdminResetLinkMethod(hashedToken, now);

        PasswordRecoveryRequest savedRecoveryRequest =
                passwordRecoveryRequestRepository.save(recoveryRequest);

        return new PasswordResetLinkAdminResult(
                buildResetLink(rawToken),
                savedRecoveryRequest
        );
    }



    @Transactional(readOnly = true)
    public List<AdminPasswordRecoveryRequestResult> findAllRecoveryRequestsByAdmin() {
        return AdminPasswordRecoveryRequestResult.from(
                passwordRecoveryRequestRepository.findAll()
        );
    }

    @Transactional(readOnly = true)
    public AdminPasswordRecoveryRequestResult findRecoveryRequestByIdByAdmin(Long requestId) {
        PasswordRecoveryRequest request = findPasswordResetRequestById(requestId);

        return AdminPasswordRecoveryRequestResult.from(request);
    }

    @Transactional(readOnly = true)
    public List<AdminPasswordRecoveryRequestResult> findRecoveryRequestsByUserIdByAdmin(Long userId) {
        ensureUserExists(userId);

        return AdminPasswordRecoveryRequestResult.from(
                passwordRecoveryRequestRepository.findByUserId(userId)
        );
    }

    @Transactional
    public AdminPasswordRecoveryRequestResult cancelRecoveryRequestByAdmin(Long requestId) {
        PasswordRecoveryRequest request = findPasswordResetRequestById(requestId);
        Instant now = Instant.now(clock);

        request.cancel(now);

        PasswordRecoveryRequest savedRequest = passwordRecoveryRequestRepository.save(request);

        return AdminPasswordRecoveryRequestResult.from(savedRequest);
    }



    private PasswordRecoveryRequest findPasswordResetRequestById(Long requestId) {
        return passwordRecoveryRequestRepository.findById(requestId)
                .orElseThrow(() -> new PasswordRecoveryRequestNotFoundException(requestId));
    }

    private void ensureUserExists(Long userId) {
        if (userRepository.findById(userId).isEmpty()) {
            throw new UserIdNotFoundException(userId);
        }
    }


    private PasswordRecoveryRequest getOrCreateOpenRecoveryRequest(
            Long userId,
            Instant now
    ) {
        return passwordRecoveryRequestRepository
                .findOpenByUserId(userId, now)
                .orElseGet(() -> createOpenRecoveryRequest(userId, now));
    }

    private PasswordRecoveryRequest createOpenRecoveryRequest(
            Long userId,
            Instant now
    ) {
        Instant expiresAt = now.plus(authTimeProperties.passwordRecoveryRequestTtl());

        PasswordRecoveryRequest recoveryRequest =
                PasswordRecoveryRequest.createOpenRequest(
                        userId,
                        now,
                        expiresAt
                );

        return passwordRecoveryRequestRepository.save(recoveryRequest);
    }

    private String buildResetLink(String rawToken) {
        return applicationUrlProperties.baseUrl()
                + "/password-recovery/reset?token="
                + rawToken;
    }
}