package com.jeepclub.backend.authentication.core.application.service;

import com.jeepclub.backend.authentication.core.application.exceptions.user.UserIdNotFoundException;
import com.jeepclub.backend.authentication.core.application.result.TemporaryPasswordAdminResult;
import com.jeepclub.backend.authentication.core.application.result.IssuedTemporaryPassword;
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
public class GenerateTemporaryPasswordService {
    private final UserRepository userRepository;
    private final PasswordRecoveryRequestRepository requestRepository;
    private final PasswordRecoveryRequestManager requestManager;
    private final TemporaryPasswordIssuer passwordIssuer;
    private final CredentialRevocationService revocationService;
    private final Clock clock;

    @Transactional
    public TemporaryPasswordAdminResult generate(Long userId) {
        Instant now = Instant.now(clock);
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserIdNotFoundException("User target not found."));
        user.assertCanRequestPasswordChange();
        PasswordRecoveryRequest request = requestManager.getOrCreate(user.getId(), now);
        IssuedTemporaryPassword temporaryPassword = passwordIssuer.issue();
        revocationService.revokeAllForUser(user.getId(), now);
        request.changeToAdminTemporaryPasswordMethod(now);
        user.changeToTemporaryPassword(temporaryPassword.passwordHash(), now);
        userRepository.save(user);
        return new TemporaryPasswordAdminResult(
                temporaryPassword.rawPassword(), requestRepository.save(request)
        );
    }
}
