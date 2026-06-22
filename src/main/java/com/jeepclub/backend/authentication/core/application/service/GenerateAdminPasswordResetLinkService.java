package com.jeepclub.backend.authentication.core.application.service;

import com.jeepclub.backend.authentication.core.application.exceptions.user.UserIdNotFoundException;
import com.jeepclub.backend.authentication.core.application.result.PasswordResetLinkAdminResult;
import com.jeepclub.backend.authentication.core.application.result.IssuedPasswordResetToken;
import com.jeepclub.backend.authentication.core.domain.model.PasswordRecoveryRequest;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.repository.PasswordRecoveryRequestRepository;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class GenerateAdminPasswordResetLinkService {
    private final UserRepository userRepository;
    private final PasswordRecoveryRequestRepository requestRepository;
    private final PasswordRecoveryRequestManager requestManager;
    private final PasswordResetTokenIssuer tokenIssuer;
    private final Clock clock;

    @Transactional
    public PasswordResetLinkAdminResult generate(Long userId) {
        Instant now = Instant.now(clock);
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserIdNotFoundException("User target not found."));
        user.assertCanRequestPasswordChange();
        PasswordRecoveryRequest request = requestManager.getOrCreate(user.getId(), now);
        IssuedPasswordResetToken token = tokenIssuer.issue();
        request.changeToAdminResetLinkMethod(token.tokenHash(), now);
        PasswordRecoveryRequest saved = requestRepository.save(request);
        return new PasswordResetLinkAdminResult(
                token.resetLink(),
                saved
        );
    }
}
