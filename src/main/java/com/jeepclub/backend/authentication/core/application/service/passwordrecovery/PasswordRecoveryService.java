package com.jeepclub.backend.authentication.core.application.service.passwordrecovery;

import com.jeepclub.backend.authentication.core.application.exceptions.tokenhash.TokenInvalidException;
import com.jeepclub.backend.authentication.core.application.exceptions.tokenhash.TokenNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserIdNotFoundException;
import com.jeepclub.backend.authentication.core.application.result.IssuedPasswordResetToken;
import com.jeepclub.backend.authentication.core.application.result.PublicPasswordRecoveryResult;
import com.jeepclub.backend.authentication.core.application.service.internal.CredentialRevocationService;
import com.jeepclub.backend.authentication.core.application.service.internal.PasswordRecoveryRequestManager;
import com.jeepclub.backend.authentication.core.application.service.internal.PasswordResetTokenIssuer;
import com.jeepclub.backend.authentication.core.domain.model.PasswordRecoveryRequest;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.port.NotificationPort;
import com.jeepclub.backend.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.authentication.core.port.RefreshTokenHashService;
import com.jeepclub.backend.authentication.core.repository.PasswordRecoveryRequestRepository;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {

    private final UserRepository userRepository;
    private final PasswordRecoveryRequestRepository requestRepository;
    private final PasswordRecoveryRequestManager requestManager;
    private final NotificationPort notificationPort;
    private final PasswordResetTokenIssuer tokenIssuer;
    private final RefreshTokenHashService tokenHashService;
    private final PasswordHasher passwordHasher;
    private final CredentialRevocationService revocationService;
    private final Clock clock;

    @Transactional
    public PublicPasswordRecoveryResult request(String cpf) {
        Instant now = Instant.now(clock);
        User user = userRepository.findByCpfForUpdate(cpf).orElse(null);
        if (user != null && !user.isDisabled()) {
            requestManager.getOrCreate(user.getId(), now);
        }
        return requestManager.genericResult(now);
    }

    @Transactional
    public PublicPasswordRecoveryResult sendEmailToken(String cpf) {
        Instant now = Instant.now(clock);
        User user = userRepository.findByCpfForUpdate(cpf).orElse(null);
        if (user == null || user.isDisabled()
                || user.getEmail() == null || user.getEmail().isBlank()) {
            return requestManager.genericEmailResult(now);
        }
        PasswordRecoveryRequest request = requestManager.getOrCreate(user.getId(), now);
        IssuedPasswordResetToken token = tokenIssuer.issue();
        request.changeToEmailTokenMethod(token.tokenHash(), now);
        requestRepository.save(request);
        notificationPort.sendPasswordResetLink(
                user.getEmail(),
                token.resetLink()
        );
        return requestManager.genericEmailResult(now);
    }

    @Transactional
    public void resetPasswordByToken(String rawToken, String newPassword) {
        Instant now = Instant.now(clock);
        String tokenHash = tokenHashService.hash(rawToken);
        Long userId = requestRepository.findUserIdByTokenHash(tokenHash)
                .orElseThrow(() -> new TokenNotFoundException("Token invalid or expired."));
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserIdNotFoundException("User not found with this id."));
        PasswordRecoveryRequest request = requestRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new TokenNotFoundException("Token invalid or expired."));
        if (!request.getUserId().equals(user.getId())
                || !request.isTokenBased() || !request.isOpen(now)) {
            throw new TokenInvalidException("Token invalid or expired.");
        }
        user.assertCanRequestPasswordChange();
        revocationService.revokeAllForUser(user.getId(), now);
        user.changePassword(passwordHasher.hash(newPassword), now);
        request.resolve(now);
        userRepository.save(user);
        requestRepository.save(request);
    }
}
