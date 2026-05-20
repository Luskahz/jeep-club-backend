package com.jeepclub.backend.authentication.core.application.services;

import com.jeepclub.backend.authentication.core.application.exceptions.tokenhash.TokenInvalidException;
import com.jeepclub.backend.authentication.core.application.exceptions.tokenhash.TokenNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserIdNotFoundException;
import com.jeepclub.backend.authentication.core.application.results.PasswordResetTokenAdminResult;
import com.jeepclub.backend.authentication.core.application.results.TemporaryPasswordAdminResult;
import com.jeepclub.backend.authentication.core.domain.model.PasswordResetRequest;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.port.*;
import com.jeepclub.backend.authentication.core.repository.PasswordResetRequestRepository;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {

    private final UserRepository userRepository;
    private final PasswordResetRequestRepository passwordResetRepository;
    private final NotificationPort notificationPort;
    private final RandomPasswordGenerator randomPasswordGenerator;
    private final RefreshTokenGenerator tokenGenerator;
    private final RefreshTokenHashService tokenHashService;
    private final PasswordHasher passwordHasher;
    private final ApplicationTimeProperties authTimeProperties;
    private final ApplicationUrlProperties applicationUrlProperties;
    private final Clock clock;

    @Transactional
    public void requestRecoveryViaEmail(String cpf) {
        Instant now = Instant.now(clock);

        userRepository.findByCpf(cpf)
                .ifPresent(user -> {
                    user.assertCanRequestPasswordChange();

                    String rawToken = tokenGenerator.generate();
                    String hashedToken = tokenHashService.hash(rawToken);

                    Instant expiresAt = now.plus(authTimeProperties.passwordChangeRequestTtl());

                    PasswordResetRequest resetRequest = PasswordResetRequest.create(
                            user.getId(),
                            hashedToken,
                            now,
                            expiresAt
                    );

                    passwordResetRepository.save(resetRequest);

                    String resetLink = applicationUrlProperties.baseUrl()
                            + "/reset-password?token="
                            + rawToken;

                    notificationPort.sendPasswordResetLink(user.getEmail(), resetLink);
                });
    }

    @Transactional
    public TemporaryPasswordAdminResult generateTemporaryPasswordByAdmin(Long targetUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserIdNotFoundException("User target not found."));

        user.assertCanRequestPasswordChange();

        Instant now = Instant.now(clock);

        String temporaryPassword = randomPasswordGenerator.generateSecurePassword();
        String temporaryPasswordHash = passwordHasher.hash(temporaryPassword);

        user.changePassword(temporaryPasswordHash, now);

        userRepository.save(user);

        return new TemporaryPasswordAdminResult(temporaryPassword);
    }

    @Transactional
    public PasswordResetTokenAdminResult generateResetTokenByAdmin(Long targetUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserIdNotFoundException("User target not found."));

        user.assertCanRequestPasswordChange();

        Instant now = Instant.now(clock);

        String rawToken = tokenGenerator.generate();
        String hashedToken = tokenHashService.hash(rawToken);

        Instant expiresAt = now.plus(authTimeProperties.passwordChangeRequestTtl());

        PasswordResetRequest resetRequest = PasswordResetRequest.create(
                user.getId(),
                hashedToken,
                now,
                expiresAt
        );

        passwordResetRepository.save(resetRequest);

        return new PasswordResetTokenAdminResult(rawToken);
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        Instant now = Instant.now(clock);

        String hashedToken = tokenHashService.hash(rawToken);

        PasswordResetRequest resetRequest = passwordResetRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new TokenNotFoundException("Token invalid or expired."));

        if (!resetRequest.isPending(now)) {
            throw new TokenInvalidException("Token invalid or expired.");
        }

        User user = userRepository.findById(resetRequest.getUserId())
                .orElseThrow(() -> new UserIdNotFoundException("User not found with this id."));

        String newPasswordHash = passwordHasher.hash(newPassword);

        user.changePassword(newPasswordHash, now);
        resetRequest.markAsUsed(now);

        userRepository.save(user);
        passwordResetRepository.save(resetRequest);
    }
}