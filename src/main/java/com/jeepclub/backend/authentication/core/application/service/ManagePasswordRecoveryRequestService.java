package com.jeepclub.backend.authentication.core.application.service;

import com.jeepclub.backend.authentication.core.application.exceptions.login.PasswordRecoveryRequestNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserIdNotFoundException;
import com.jeepclub.backend.authentication.core.application.result.admin.recovery.AdminPasswordRecoveryRequestResult;
import com.jeepclub.backend.authentication.core.domain.model.PasswordRecoveryRequest;
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
public class ManagePasswordRecoveryRequestService {
    private final UserRepository userRepository;
    private final PasswordRecoveryRequestRepository requestRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<AdminPasswordRecoveryRequestResult> findAll() {
        return AdminPasswordRecoveryRequestResult.from(requestRepository.findAll());
    }

    @Transactional(readOnly = true)
    public AdminPasswordRecoveryRequestResult findById(Long id) {
        return AdminPasswordRecoveryRequestResult.from(find(id));
    }

    @Transactional(readOnly = true)
    public List<AdminPasswordRecoveryRequestResult> findByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserIdNotFoundException(userId);
        }
        return AdminPasswordRecoveryRequestResult.from(requestRepository.findByUserId(userId));
    }

    @Transactional
    public AdminPasswordRecoveryRequestResult cancel(Long id) {
        PasswordRecoveryRequest request = requestRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new PasswordRecoveryRequestNotFoundException(id));
        request.cancel(Instant.now(clock));
        return AdminPasswordRecoveryRequestResult.from(requestRepository.save(request));
    }

    private PasswordRecoveryRequest find(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new PasswordRecoveryRequestNotFoundException(id));
    }
}
