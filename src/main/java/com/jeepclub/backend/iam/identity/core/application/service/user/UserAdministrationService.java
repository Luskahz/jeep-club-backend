package com.jeepclub.backend.iam.identity.core.application.service.user;

import com.jeepclub.backend.iam.identity.core.domain.exception.UserAlreadyDisabledException;
import com.jeepclub.backend.iam.identity.core.domain.exception.UserNotDisabledException;
import com.jeepclub.backend.iam.identity.api.module.UserAdministration;
import com.jeepclub.backend.iam.identity.api.module.UserDetails;
import com.jeepclub.backend.iam.identity.api.module.exception.UserNotFoundException;
import com.jeepclub.backend.iam.identity.core.domain.model.User;
import com.jeepclub.backend.iam.identity.api.module.spi.UserAuthenticationAdministrationPort;
import com.jeepclub.backend.iam.identity.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
class UserAdministrationService implements UserAdministration {

    private final UserRepository userRepository;
    private final UserAuthenticationAdministrationPort authenticationAdministrationPort;

    @Override
    @Transactional
    public UserDetails disable(Long userId, Instant now) {
        User user = findForUpdate(userId);
        try {
            user.disable(now);
        } catch (UserAlreadyDisabledException exception) {
            throw new com.jeepclub.backend.iam.identity.api.module.exception.UserAlreadyDisabledException(userId, exception);
        }

        User saved = userRepository.save(user);
        authenticationAdministrationPort.disableAuthentication(userId, now);
        return UserQueryService.toDetails(saved);
    }

    @Override
    @Transactional
    public UserDetails enable(Long userId, Instant now) {
        User user = findForUpdate(userId);
        try {
            user.enable(now);
        } catch (UserNotDisabledException exception) {
            throw new com.jeepclub.backend.iam.identity.api.module.exception.UserNotDisabledException(userId, exception);
        }

        User saved = userRepository.save(user);
        authenticationAdministrationPort.enableAuthentication(userId, now);
        return UserQueryService.toDetails(saved);
    }

    private User findForUpdate(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
