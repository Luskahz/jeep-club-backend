package com.jeepclub.backend.authentication.core.application.service;

import com.jeepclub.backend.authentication.core.application.result.PublicPasswordRecoveryResult;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RequestPasswordRecoveryService {
    private final UserRepository userRepository;
    private final PasswordRecoveryRequestManager requestManager;
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
}
