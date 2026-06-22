package com.jeepclub.backend.authentication.core.application.service;

import com.jeepclub.backend.authentication.core.application.result.PublicPasswordRecoveryResult;
import com.jeepclub.backend.authentication.core.domain.model.PasswordRecoveryRequest;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.application.result.IssuedPasswordResetToken;
import com.jeepclub.backend.authentication.core.port.NotificationPort;
import com.jeepclub.backend.authentication.core.repository.PasswordRecoveryRequestRepository;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SendPasswordRecoveryEmailService {
    private final UserRepository userRepository;
    private final PasswordRecoveryRequestRepository requestRepository;
    private final PasswordRecoveryRequestManager requestManager;
    private final NotificationPort notificationPort;
    private final PasswordResetTokenIssuer tokenIssuer;
    private final Clock clock;

    @Transactional
    public PublicPasswordRecoveryResult send(String cpf) {
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
}
