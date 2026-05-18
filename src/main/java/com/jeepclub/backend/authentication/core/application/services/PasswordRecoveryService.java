package com.jeepclub.backend.authentication.core.application.services;

import com.jeepclub.backend.authentication.core.application.exceptions.user.UserNotFoundException;
import com.jeepclub.backend.authentication.core.application.results.PasswordRecoveryAdminResult;
import com.jeepclub.backend.authentication.core.domain.model.PasswordResetRequest;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.port.*;
import com.jeepclub.backend.authentication.core.repository.PasswordResetRequestRepository;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {

    private final UserRepository userRepository;
    private final PasswordResetRequestRepository passwordResetRepository;
    private final NotificationPort notificationPort;
    private final RandomPasswordGenerator randomPasswordGenerator;
    private final RefreshTokenGenerator tokenGenerator; // Reusing to generate secure random strings for the token
    private final RefreshTokenHashService tokenHashService; // Reusing to hash the token
    private final PasswordHasher passwordHasher;
    private final AuthTimeProperties authTimeProperties;

    @Transactional
    public void requestRecoveryViaEmail(String cpf) {
        User user = userRepository.findByCpf(cpf)
                .orElseThrow(() -> new UserNotFoundException("CPF não encontrado"));

        user.assertCanRequestPasswordChange();

        String rawToken = tokenGenerator.generate();
        String hashedToken = tokenHashService.hash(rawToken);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(authTimeProperties.passwordChangeRequestTtl());

        PasswordResetRequest resetRequest = PasswordResetRequest.create(
                user.getId(),
                hashedToken,
                now,
                expiresAt
        );

        passwordResetRepository.save(resetRequest);

        // Simulando a construção do link
        String resetLink = "http://jeepclub.com/reset-password?token=" + rawToken;
        notificationPort.sendPasswordResetLink(user.getEmail(), resetLink);
    }

    @Transactional
    public PasswordRecoveryAdminResult requestRecoveryViaAdmin(Long targetUserId, boolean generateTempPassword) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário alvo não encontrado"));

        user.assertCanRequestPasswordChange();
        Instant now = Instant.now();

        if (generateTempPassword) {
            String tempPassword = randomPasswordGenerator.generateSecurePassword();
            String tempPasswordHash = passwordHasher.hash(tempPassword);
            user.changePassword(tempPasswordHash, now);
            userRepository.save(user);
            return new PasswordRecoveryAdminResult(tempPassword, null);
        } else {
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
            return new PasswordRecoveryAdminResult(null, rawToken);
        }
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        String hashedToken = tokenHashService.hash(rawToken);
        
        PasswordResetRequest resetRequest = passwordResetRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido ou expirado"));

        Instant now = Instant.now();
        
        if (!resetRequest.isPending(now)) {
            throw new IllegalArgumentException("Token inválido ou expirado");
        }

        User user = userRepository.findById(resetRequest.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        String newPasswordHash = passwordHasher.hash(newPassword);
        user.changePassword(newPasswordHash, now);
        
        resetRequest.markAsUsed(now);
        
        userRepository.save(user);
        passwordResetRepository.save(resetRequest);
    }
}
