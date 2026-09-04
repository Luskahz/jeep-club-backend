package com.jeepclub.backend.identity.core.application.service.user;

import com.jeepclub.backend.identity.api.module.UserAdministration;
import com.jeepclub.backend.identity.api.module.UserDetails;
import com.jeepclub.backend.identity.api.module.exception.UserAlreadyDisabledException;
import com.jeepclub.backend.identity.api.module.exception.UserNotDisabledException;
import com.jeepclub.backend.identity.api.module.exception.UserNotFoundException;
import com.jeepclub.backend.identity.core.domain.model.User;
import com.jeepclub.backend.identity.api.module.spi.UserAuthenticationAdministrationPort;
import com.jeepclub.backend.identity.core.repository.UserRepository;
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
    public UserDetails disable(Long identityId, Instant now) {
        User user = findForUpdate(identityId);
        try {
            user.disable(now);
        } catch (com.jeepclub.backend.identity.core.domain.exception.UserAlreadyDisabledException exception) {
            throw new UserAlreadyDisabledException(identityId, exception);
        }

        User saved = userRepository.save(user);
        authenticationAdministrationPort.disableAuthentication(identityId, now);
        return UserQueryService.toDetails(saved);
    }

    @Override
    @Transactional
    public UserDetails enable(Long identityId, Instant now) {
        User user = findForUpdate(identityId);
        try {
            user.enable(now);
        } catch (com.jeepclub.backend.identity.core.domain.exception.UserNotDisabledException exception) {
            throw new UserNotDisabledException(identityId, exception);
        }

        User saved = userRepository.save(user);
        authenticationAdministrationPort.enableAuthentication(identityId, now);
        return UserQueryService.toDetails(saved);
    }

    private User findForUpdate(Long identityId) {
        return userRepository.findByIdForUpdate(identityId)
                .orElseThrow(() -> new UserNotFoundException(identityId));
    }
}
