package com.jeepclub.backend.authentication.core.application.service.internal;

import com.jeepclub.backend.authentication.core.application.result.PublicPasswordRecoveryResult;
import com.jeepclub.backend.authentication.core.domain.model.PasswordRecoveryRequest;
import com.jeepclub.backend.authentication.core.domain.enums.PasswordRecoveryRequestMethod;
import com.jeepclub.backend.authentication.core.port.ApplicationTimeProperties;
import com.jeepclub.backend.authentication.core.repository.PasswordRecoveryRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class PasswordRecoveryRequestManager {
    private final PasswordRecoveryRequestRepository repository;
    private final ApplicationTimeProperties timeProperties;

    public PasswordRecoveryRequest getOrCreate(Long userId, Instant now) {
        return repository.findOpenByUserIdForUpdate(userId, now)
                .orElseGet(() -> repository.save(PasswordRecoveryRequest.createOpenRequest(
                        userId, now, now.plus(timeProperties.passwordRecoveryRequestTtl())
                )));
    }

    public PublicPasswordRecoveryResult genericResult(Instant now) {
        return PublicPasswordRecoveryResult.pending(
                now, now.plus(timeProperties.passwordRecoveryRequestTtl())
        );
    }

    public PublicPasswordRecoveryResult genericEmailResult(Instant now) {
        return PublicPasswordRecoveryResult.pending(
                now,
                now.plus(timeProperties.passwordRecoveryRequestTtl()),
                PasswordRecoveryRequestMethod.EMAIL_TOKEN
        );
    }
}
