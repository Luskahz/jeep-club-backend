package com.jeepclub.backend.authorization.infra.authentication;

import com.jeepclub.backend.authorization.core.port.UserIdentityProvider;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationUserIdentityProvider implements UserIdentityProvider {

    private final UserRepository userRepository;

    @Override
    public boolean existsById(Long userId) {
        return userRepository.existsById(userId);
    }
}